package net.mosstest.tests;

import net.mosstest.scripting.MapChunk;
import net.mosstest.scripting.Position;
import net.mosstest.servercore.serialization.IByteArrayWritable;
import net.mosstest.servercore.serialization.LevelDBBackedMap;
import net.mosstest.servercore.MapGeneratorException;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.util.Arrays;

public class LevelDBBackedMapTest {

    @Test
    public void testBasicStore() throws MapGeneratorException, IOException {
        LevelDBBackedMap<TestByteArrayStorable, TestByteArrayStorable> map = new LevelDBBackedMap<>(new MockDB(), TestByteArrayStorable::new);
        TestByteArrayStorable testKey = new TestByteArrayStorable(new byte[]{1,2});
       TestByteArrayStorable testVal = new TestByteArrayStorable(new byte[]{3,4});
        map.put(testKey, testVal);
        TestByteArrayStorable vOut = map.get(testKey);
        Assert.assertTrue(vOut.equals(testVal));
    }
    @Test
    public void testDbLookup() throws IOException {
        LevelDBBackedMap<TestByteArrayStorable, TestByteArrayStorable> map = new LevelDBBackedMap<>(new MockDB(), TestByteArrayStorable::new);
        TestByteArrayStorable vOut = map.get(new TestByteArrayStorable(new byte[]{1,2}));
        Assert.assertNotNull(vOut);
    }

    @Test(expected = ClassCastException.class)
    public void testUncheckedKey() throws MapGeneratorException {
        LevelDBBackedMap<Position, MapChunk> map = new LevelDBBackedMap<>(new MockDB(), MapChunk::new);

        Object notAPosition = new Object();

        MapChunk cOut = map.get(notAPosition);
    }


    public static class TestByteArrayStorable implements IByteArrayWritable {
        byte[] buf;

        public TestByteArrayStorable(byte[] buf) {

            buf = buf;
        }

        @Override
        public byte[] toBytes() {
            return buf;
        }




        public TestByteArrayStorable() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestByteArrayStorable that = (TestByteArrayStorable) o;

            if (!Arrays.equals(buf, that.buf)) return false;

            return true;
        }

        @Override
        public String toString() {
            return "TestByteArrayStorable{" +
                    "buf=" + Arrays.toString(buf) +
                    '}';
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(buf);
        }
    }
}
