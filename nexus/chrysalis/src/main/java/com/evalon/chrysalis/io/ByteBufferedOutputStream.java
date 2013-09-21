/**
 * 
 */
package com.evalon.chrysalis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class ByteBufferedOutputStream extends OutputStream
{
	private ByteBuffer	buffer;
	private int			pos			= -1;
	private boolean		isClosed	= false;
	
	/**
	 * Creates an instance that positions the stream at the start of the buffer.
	 */
	public ByteBufferedOutputStream(ByteBuffer buf)
	{
		this(buf, 0);
	}
	
	/**
	 * Creates an instance that positions the stream at the specified offset.
	 */
	public ByteBufferedOutputStream(ByteBuffer buf, int off)
	{
		buffer = buf;
		buffer.position(off);
	}
	
	@Override
	public void write(int b) throws IOException
	{
		if (isClosed)
		{
			throw new IOException("Stream is closed");
		}
		
		buffer.put((byte) (b & 0xFF));
	}
	
	@Override
	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (isClosed)
		{
			throw new IOException("Stream is closed");
		}
		
		buffer.put(b, off, len);
	}
	
	public synchronized void reset() throws IOException
	{
		if (pos < 0)
		{
			throw new IOException("Illegal Possition " + pos);
		}
		
		buffer.position(pos);
	}
	
	@Override
	public void flush() throws IOException
	{
		
	}
	
	public boolean isClosed()
	{
		return this.isClosed;
	}
	
	public void setPos(int pos)
	{
		this.pos = pos;
	}
	
	public int getPos()
	{
		return this.pos;
	}
	
	@Override
	public void close() throws IOException
	{
		this.isClosed = true;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException
	{
		return this.buffer.duplicate();
	}
	
	public ByteBuffer getBuffer()
	{
		return this.buffer;
	}
	
}
