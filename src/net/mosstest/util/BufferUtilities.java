package net.mosstest.util;

import io.netty.buffer.ByteBuf;

/**
 * Created by hexafraction on 8/25/14.
 */
public class BufferUtilities {
    /**
     * Reads a Java string up to Integer.MAX_VALUE characters in length encoded using writeUTF.
     *
     * @param buf The buffer to read. It will be read at the current read pointer.
     * @return The string that had been read.
     * @throws IndexOutOfBoundsException Thrown if the buffer is exhausted before reading is complete.
     */
    public static String readUTF(ByteBuf buf) throws IndexOutOfBoundsException {
        int length = buf.readInt();
        char[] characters = new char[length];
        for (int i = 0; i < length; i++) {
            characters[i] = buf.readChar();
        }
        return new String(characters);
    }

    /**
     * Writes a Java string up to Integer.MAX_VALUE characters in length, encoded by first writing the string length
     * in the buffer's specified endianness, then writing the string's characters.
     *
     * @param buf The buffer to write to
     * @param s The string to write
     * @throws IndexOutOfBoundsException Thrown if there are not enough writable bytes in the buffer.
     */
    public static void writeUTF(ByteBuf buf, String s) throws IndexOutOfBoundsException {
        char[] characters = s.toCharArray();
        buf.writeInt(characters.length);
        for(int i = 0; i < characters.length; i++){
            buf.writeChar(characters[i]);
        }
    }
}
