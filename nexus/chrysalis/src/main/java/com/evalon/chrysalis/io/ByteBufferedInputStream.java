/**
 * 
 */
package com.evalon.chrysalis.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class ByteBufferedInputStream extends InputStream
{
	private ByteBuffer	buffer;
	private int			pos			= -1;
	private boolean		isClosed	= false;
	
	/**
	 * Creates an instance that positions the stream at the start of the buffer.
	 */
	public ByteBufferedInputStream(ByteBuffer buf)
	{
		this(buf, 0);
	}
	
	/**
	 * Creates an instance that positions the stream at the specified offset.
	 */
	public ByteBufferedInputStream(ByteBuffer buf, int off)
	{
		buffer = buf;
		buffer.position(off);

	}
	
	@Override
	public int available() throws IOException
	{
		return buffer.limit() - buffer.position();
	}
	
	@Override
	public void close() throws IOException
	{
		isClosed = true;
	}
	
	@Override
	public synchronized void mark(int readlimit)
	{
		pos = buffer.position();
	}
	
	@Override
	public boolean markSupported()
	{
		return true;
	}
	
	@Override
	public int read() throws IOException
	{
		if (isClosed)
		{
			throw new IOException("Stream is closed");
		}
		
		if (available() <= 0)
		{
			return -1;
		}
		
		return buffer.get() & 0xFF;
	}
	
	@Override
	public int read(byte[] b) throws IOException
	{
		return read(b, 0, b.length);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (isClosed)
		{
			throw new IOException("Stream is closed");
		}
		
		int bytes = Math.min(len, available());
		if (bytes == 0)
		{
			return -1;
		}
		
		buffer.get(b, off, bytes);
		return bytes;
	}
	
	@Override
	public synchronized void reset() throws IOException
	{
		if (pos < 0)
		{
			throw new IOException("Illegal Possition " + pos);
		}
		
		buffer.position(pos);
	}
	
	@Override
	public long skip(long i) throws IOException
	{
		int ii = (i > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) i;
		int iii = Math.min(ii, available());
		int p = iii + buffer.position();
		buffer.position(p);
		return (long) iii;
	}
	
	public void setPos(int pos)
	{
		this.pos = pos;
	}
	
	public int getPos()
	{
		return this.pos;
	}
	
	public boolean isClosed()
	{
		return this.isClosed;
	}
	
	public ByteBuffer getBuffer()
	{
		return this.buffer;
	}
}
