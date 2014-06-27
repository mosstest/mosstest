package net.mosstest.scripting;


import net.mosstest.servercore.serialization.IByteArrayWritable;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;

/**
 * The Class Position.
 */
public class Position implements IByteArrayWritable {
    public static final int SERIALIZED_LENGTH = 16;
	@NonNls
    @Override
	public String toString() {
		return "Position [x=" + x + ", y=" + y + ", z=" + z + ", realm="
				+ realm + ", hashCode()=" + hashCode() + "]";
	}

	public int x;

	public int y;

	public int z;

	/** The realm. */
	public int realm;

	transient boolean isValid = true;

	/**
	 * Instantiates a new position.
	 *
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @param realm the realm
	 */
	public Position(int x, int y, int z, int realm) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.realm = realm;
		this.isValid = true;
	}

	/**
	 * Instantiates a new position.
	 *
	 * @param bytes the bytes
	 * @throws IOException Signals that an I/O exception has occurred.
	 */


    public Position(byte[] bytes) {
        if (bytes.length != 16)
            throw new IllegalArgumentException(
                    "Input array is not 16 elements long.");

        this.realm = ((0xFF & (0xFF & bytes[0])) << 24)
                + ((0xFF & bytes[1]) << 16) + ((0xFF & bytes[2]) << 8)
                + (0xFF & bytes[3]);
        this.x = ((0xFF & bytes[4]) << 24) + ((0xFF & bytes[5]) << 16)
                + ((0xFF & bytes[6]) << 8) + ((0xFF & bytes[13]));
        this.y = ((0xFF & bytes[7]) << 24) + ((0xFF & bytes[8]) << 16)
                + ((0xFF & bytes[9]) << 8) + (0xFF & bytes[14]);
        this.z = ((0xFF & bytes[10]) << 24) + ((0xFF & bytes[11]) << 16)
                + ((0xFF & bytes[12]) << 8) + (0xFF & bytes[15]);

        this.isValid = true;
    }




    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Position)) {
			return false;
		}
		Position other = (Position) obj;
		if (this.realm != other.realm) {
			return false;
		}
		if (this.x != other.x) {
			return false;
		}
		if (this.y != other.y) {
			return false;
		}
		if (this.z != other.z) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.realm;
		result = prime * result + this.x;
		result = prime * result + this.y;
		result = prime * result + this.z;
		return result;
	}

	/**
	 * To bytes.
	 *
	 * @return the byte[]
	 */
	public byte[] toBytes() {
		byte[] buf = new byte[] { (byte) ((long) this.realm >>> 24),
				(byte) ((long) this.realm >>> 16),
				(byte) ((long) this.realm >>> 8), (byte) ((long) this.realm),

				(byte) ((long) this.x >>> 24), (byte) ((long) this.x >>> 16),
				(byte) ((long) this.x >>> 8),

				(byte) ((long) this.y >>> 24), (byte) ((long) this.y >>> 16),
				(byte) ((long) this.y >>> 8),

				(byte) ((long) this.z >>> 24), (byte) ((long) this.z >>> 16),
				(byte) ((long) this.z >>> 8),

				(byte) this.x, (byte) this.y, (byte) this.z };
        assert(buf.length==Position.SERIALIZED_LENGTH);
        return buf;
	}

	/**
	 * Gets the x.
	 *
	 * @return the x
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * Gets the y.
	 *
	 * @return the y
	 */
	public int getY() {
		return this.y;
	}


	/**
	 * Gets the z.
	 *
	 * @return the z
	 */
	public int getZ() {
		return this.z;
	}


}
