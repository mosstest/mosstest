package net.mosstest.scripting;

import static org.fusesource.leveldbjni.JniDBFactory.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import net.mosstest.servercore.MosstestFatalDeathException;
import net.mosstest.servercore.serialization.IByteArrayWritable;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;


// TODO: Auto-generated Javadoc

/**
 * The Class ScriptableDatabase.
 */
public class ScriptableDatabase {
    private static final Logger logger = Logger.getLogger(ScriptableDatabase.class);
    /**
     * The base dir.
     */
    File baseDir;

    /**
     * Instantiates a new scriptable database.
     *
     * @param baseDir the base dir
     */
    public ScriptableDatabase(File baseDir) {
        this.baseDir = baseDir;

    }

    /**
     * Gets the db.
     *
     * @param name the name
     * @return the db
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public DBase getDb(String name) throws IOException {
        if (!name.matches("[a-zA-Z]{1,32}")) { //$NON-NLS-1$
            throw new IllegalArgumentException(Messages.getString("ScriptableDatabase.DB_NAME_INVALID")); //$NON-NLS-1$
        }
        Options options = new Options();
        options.createIfMissing(true);
        return new DBase(factory.open(new File(this.baseDir, name //$NON-NLS-1$
                + ".scriptdb"), options), name); //$NON-NLS-1$

    }


    public class DBase {
        // this class will contain a database that scripts may access.
        /**
         * The inner db.
         */
        private final DB innerDb;

        /**
         * Instantiates a new database.
         *
         * @param innerDb the inner db
         * @param name    the name
         */
        DBase(DB innerDb, String name) {
            this.innerDb = innerDb;
        }


    }

    /**
     * Workaround class for java.lang.String being declared final.
     */
    public static final class DBString implements IByteArrayWritable, CharSequence {

        private final String s;

        // does not match for an equal string to keep this method's symmetric property
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof DBString)) return false;

            DBString dbString = (DBString) o;

            if (!s.equals(dbString.s)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        @Override
        public IntStream chars() {
            return s.chars();
        }

        @Override
        public CharSequence subSequence(int beginIndex, int endIndex) {
            return s.subSequence(beginIndex, endIndex);
        }

        @Override
        public char charAt(int index) {
            return s.charAt(index);
        }

        public DBString(String s) {
            this.s = s;
        }

        @Override
        public int length() {
            return s.length();
        }

        @Override
        public byte[] toBytes() {
            return bytes(s);
        }

    }

    public static final class DBKey implements IByteArrayWritable {
        private List<IByteArrayWritable> qualifiers;

        @Override
        public byte[] toBytes() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dOut = new DataOutputStream(out);
            try {
                for (IByteArrayWritable qual : qualifiers) {
                    byte[] buf = qual.toBytes();
                    dOut.writeInt(buf.length);

                    dOut.write(buf);
                }
            } catch (IOException e) {
                logger.fatal("Error serializing scriptable DB key. THE WORLD IS GOING DOWN SHORTLY.");
                throw new MosstestFatalDeathException(e);
            }

            return null;
        }

        public DBKey(IByteArrayWritable... qualifiers) {
            this.qualifiers = new ArrayList<>(Arrays.asList(qualifiers));
        }

    }
}
