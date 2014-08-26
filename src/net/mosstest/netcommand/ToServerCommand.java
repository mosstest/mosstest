package net.mosstest.netcommand;

import io.netty.buffer.ByteBuf;

/**
 * The Class ToServerCommand.
 */
public abstract class ToServerCommand {

	public abstract ByteBuf toBytes();

}
