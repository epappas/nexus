/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectBytesArray implements DirectArray<byte[]>
{
	private int			size;
	private int			readCursor;
	private int			writeCursor;
	private DirectBytes	directBytes[];
	
	public DirectBytesArray()
	{
		this(0);
	}
	
	public DirectBytesArray(int capacity)
	{
		this.size = capacity;
		this.readCursor = 0;
		this.writeCursor = 0;
		this.directBytes = new DirectBytes[capacity];
	}
	
	public synchronized long set(byte[][] obj)
	{
		this.size = obj.length;
		this.readCursor = 0;
		this.writeCursor = 0;
		this.directBytes = new DirectBytes[this.size];
		
		long bytesToCopy = 0;
		for (this.writeCursor = 0; this.writeCursor < this.size; ++this.writeCursor)
		{
			this.directBytes[this.writeCursor] = new DirectBytes(
					obj[this.writeCursor]);
			bytesToCopy += obj[this.writeCursor].length;
		}
		return bytesToCopy * this.size;
	}
	
	public long set(byte[] obj)
	{
		if ((this.writeCursor + 1) >= this.size)
		{
			this.grow();
		}
		
		this.directBytes[++this.writeCursor] = new DirectBytes(obj);
		
		return this.writeCursor - 1L;
	}
	
	public long set(int index, byte[] obj)
	{
		boolean pass = false;
		if (index >= this.size)
		{
			this.grow();
		}
		do
		{
			try
			{
				this.directBytes[index] = new DirectBytes(obj);
				pass = true;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				pass = false;
				this.grow();
			}
		}
		while (!pass);
		
		return obj.length;
	}
	
	public byte[] get()
	{
		this.readCursor = (this.readCursor + 1) % this.size;
		DirectBytes bytes = this.directBytes[this.readCursor - 1];
		if (bytes != null)
		{
			return this.directBytes[this.readCursor - 1].get();
		}
		return null;
	}
	
	public byte[] get(int i)
	{
		return this.directBytes[i].get();
	}
	
	public void dispose()
	{
		for (DirectBytes bytes : this.directBytes)
		{
			bytes.dispose();
		}
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.dispose();
		super.finalize();
	}
	
	public long getAddress()
	{
		return this.directBytes[this.readCursor - 1].getAddress();
	}
	
	public void setAddress(long addr)
	{
		this.directBytes[this.readCursor - 1].setAddress(addr);
	}
	
	public int getSize()
	{
		return this.size;
	}
	
	public long getAddress(int i)
	{
		return this.directBytes[i].getAddress();
	}
	
	public void setAddress(int i, long addr)
	{
		this.directBytes[i].setAddress(addr);
	}
	
	public int getSize(int i)
	{
		return this.directBytes[i].getSize();
	}
	
	public synchronized byte[][] getAll()
	{
		byte[][] returnedBytes = new byte[this.size][];
		
		for (int i = 0; i < this.size; ++i)
		{
			returnedBytes[i] = this.directBytes[i].get();
		}
		
		return returnedBytes;
	}
	
	private synchronized void grow()
	{
		this.size += this.size >> 1;
		DirectBytes[] tmpArr = this.directBytes;
		this.directBytes = new DirectBytes[this.size];
		System.arraycopy(tmpArr, 0, this.directBytes, 0, tmpArr.length);
	}
	
	public byte[] pop(int i)
	{
		byte[] b = directBytes[i].get();
		
		directBytes[i].dispose();
		
		System.arraycopy(directBytes, 0, directBytes, 0, (i <= 0 ? 1 : i) - 1);
		System.arraycopy(directBytes, i + 1, directBytes, i, directBytes.length
				- i - 1);
		directBytes[directBytes.length - 1] = null;
		
		return b;
	}
	
	@Override
	public byte[] lazyPop(int i)
	{
		byte[] b = directBytes[i].get();
		
		directBytes[i].dispose();
		
		directBytes[i] = null;
		
		return b;
	}
	
}
