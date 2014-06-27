package net.mosstest.servercore.serialization;

import com.google.common.cache.*;
import net.mosstest.servercore.Messages;
import org.apache.log4j.Logger;
import org.iq80.leveldb.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * An in-memory map backed by a larger data-set stored in a levelDB byte[] to byte[] key-value store.
 *
 * @param <K> Keys to be stored
 * @param <V> Values to be stored
 */
// V need not extend any sort of marker interface, having a proper constructor passed in takes care of that.
public class LevelDBBackedMap<K extends IByteArrayWritable, V extends IByteArrayWritable> implements Map<K, V> {
    // TODO a map backed by a levelDB datastore

    // the in-memory cache
    private LoadingCache<K, V> memoryBackingCache;

    // a method reference to the constructor to be used in instantiating V from disk. ManagedMap and other subtypes may need to use this constructor.
    protected IByteArrayConstructor<V> constructor;

    protected DB diskBackingDatastore;
    private static final Logger logger = Logger.getLogger(LevelDBBackedMap.class);
    private BackedMapCacheLoader loader = new BackedMapCacheLoader();

    @Override
    public int size() {
        // memoryBackingCache.size() is a long
        if (memoryBackingCache.size() > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        else return (int) memoryBackingCache.size();
    }

    @Override
    public boolean isEmpty() {
        return memoryBackingCache.asMap().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return memoryBackingCache.asMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return memoryBackingCache.asMap().containsValue(value);
    }

    @Override
    public V get(Object key) {
        V v = null;
        try {
            v = memoryBackingCache.get((K) key);
        } catch (ExecutionException e) {
            logger.error(MessageFormat.format(Messages.getString("LDR_EXEC_EXCEPTION"), e.getMessage()));
        }
        if (v == null) {
            try {
                v = loader.load((K) key);
            } catch (Exception e) {
                logger.error(MessageFormat.format(Messages.getString("LDR_EXCEPTION"), e.getMessage()));
            }


        }
        return v;
    }

    @Override
    public V put(K key, V value) {
        memoryBackingCache.put(key, value);
        diskBackingDatastore.put(key.toBytes(), value.toBytes());
        return value;
    }


    /**
     * As this method will actually commit a deletion from the backing database,
     * it will make no sense to return the deleted value. Therefore null is returned.
     *
     * @param key The key to delete
     * @return Null at all times.
     */
    @Override
    public V remove(Object key) {
        this.memoryBackingCache.invalidate(key);
        this.memoryBackingCache.asMap().remove(key);
        if (key instanceof IByteArrayWritable)
            this.diskBackingDatastore.delete(((IByteArrayWritable) key).toBytes());
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        this.memoryBackingCache.putAll(m);
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            diskBackingDatastore.put(entry.getKey().toBytes(), entry.getValue().toBytes());
        }
    }

    /**
     * Due to limitations of levelDB the entire map cannot be cleared from disk efficiently--only the cache.
     * Use {@link LevelDBBackedMap#clearDb()} to clear the database and map.
     */
    @Override
    public void clear() {
        this.memoryBackingCache.asMap().clear();
    }

    /**
     * Clears the entire cache and database. This will use a snapshot, so any new entries added on another thread will not be deleted.
     *
     * @throws IOException
     */
    public void clearDb() throws IOException {
        this.clear();
        Snapshot s = diskBackingDatastore.getSnapshot();
        for (DBIterator dbi = diskBackingDatastore.iterator(new ReadOptions().snapshot(s)); dbi.hasNext(); ) {
            Entry<byte[], byte[]> entry = dbi.next();
            // try a remove
            dbi.remove();
            // this may cause a ConcurrentModificationException, testing needed
            diskBackingDatastore.delete(entry.getKey(), new WriteOptions().sync(false));
        }
        s.close();
    }

    @Override
    public Set<K> keySet() {
        return memoryBackingCache.asMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return memoryBackingCache.asMap().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return memoryBackingCache.asMap().entrySet();
    }

    public LevelDBBackedMap(DB diskBackingDatastore, IByteArrayConstructor<V> constructor) {
        this(diskBackingDatastore, constructor, false);
    }

    public LevelDBBackedMap(DB diskBackingDatastore, IByteArrayConstructor<V> constructor, boolean soft) {
        this.constructor = constructor;
        this.diskBackingDatastore = diskBackingDatastore;
        this.initMap0(soft);
    }

    protected LevelDBBackedMap(DB diskBackingDatastore, IByteArrayConstructor<V> constructor, boolean soft, boolean override) {
        this(diskBackingDatastore, constructor, soft);
    }

    private void initMap0(boolean soft) {
        CacheBuilder builder = CacheBuilder.newBuilder();
        if (soft) builder.softValues();

        // need this odd assignment as per javadoc of CacheBuilder#removalListener()
        CacheBuilder<K, V> checkedCacheBuilder = builder.removalListener(new BackedMapRemovalListener());

        this.memoryBackingCache = checkedCacheBuilder.build(new BackedMapCacheLoader());
    }

    protected void initMap0(boolean soft, CacheLoader<K, V> loader) {
        CacheBuilder builder = CacheBuilder.newBuilder();
        if (soft) builder.softValues();

        // need this odd assignment as per javadoc of CacheBuilder#removalListener()
        CacheBuilder<K, V> checkedCacheBuilder = builder.removalListener(new BackedMapRemovalListener());

        this.memoryBackingCache = checkedCacheBuilder.build(loader);
    }


    private class BackedMapRemovalListener implements RemovalListener<K, V> {

        @Override
        public void onRemoval(RemovalNotification<K, V> notification) {
            switch (notification.getCause()) {
                case COLLECTED:
                    logger.warn(MessageFormat.format(Messages.getString("GC_EVICT"), notification.getKey().toString()));
                    break;
                case EXPIRED:
                    logger.info(MessageFormat.format(Messages.getString("UNCACHE_EXPIRE"), notification.getKey().toString()));
                case SIZE:
                    logger.warn(MessageFormat.format(Messages.getString("EVICT_SIZE"), notification.getKey().toString()));
            }

        }
    }

    private class BackedMapCacheLoader extends CacheLoader<K, V> {


        @Override
        public V load(K k) throws Exception {
            byte[] buf = diskBackingDatastore.get(k.toBytes());
            if (buf == null) return null;
            //now we can safely pass a not-null byte array
            return LevelDBBackedMap.this.constructor.construct(diskBackingDatastore.get(k.toBytes()));
        }
    }

    public static class ManagedMap<K extends IByteArrayWritable, V extends IByteArrayWritable & IManaged<M>, M> extends LevelDBBackedMap<K, V> {
        protected M manager;

        public ManagedMap(DB diskBackingDatastore, IByteArrayConstructor<V> constructor, M manager) {
            super(diskBackingDatastore, constructor, false, true);
            this.initMap0(false, new ManagedCacheLoader());
            this.manager = manager;
        }

        public ManagedMap(DB diskBackingDatastore, IByteArrayConstructor<V> constructor, boolean soft, M manager) {
            super(diskBackingDatastore, constructor, soft, true);
            this.initMap0(soft, new ManagedCacheLoader());
            this.manager = manager;
        }

        private class ManagedCacheLoader extends CacheLoader<K, V> {

            @Override
            public V load(K key) throws Exception {
                byte[] buf = diskBackingDatastore.get(key.toBytes());
                if (buf == null) return null;
                //now we can safely pass a not-null byte array
                V v = ManagedMap.this.constructor.construct(diskBackingDatastore.get(key.toBytes()));
                v.setManager(ManagedMap.this.manager);
                return v;
            }
        }
    }
}
