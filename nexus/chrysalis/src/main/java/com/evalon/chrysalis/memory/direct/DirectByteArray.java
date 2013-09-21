/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectByteArray implements DirectArray<Byte>
{
	private int			length;
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectByteArray(byte[] obj)
	{
		this.length = obj.length;
		
		long bytesToCopy = length << 3;
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				bytesToCopy);
		for (int i = 0; i < this.length; i++)
		{
			DirectMemory.getInstance().putByte(this.heapPointer.value + i,
					obj[i]);
		}
	}
	
	public long set(Byte[] obj)
	{
		this.length = obj.length;
		
		long bytesToCopy = length << 3;
		this.heapPointer.value = DirectMemory.getInstance().reallocateMemory(
				heapPointer.value, bytesToCopy);
		for (int i = 0; i < this.length; i++)
		{
			DirectMemory.getInstance().putByte(this.heapPointer.value + i,
					obj[i]);
		}
		return bytesToCopy;
	}
	
	public long set(Byte obj)
	{
		DirectMemory.getInstance().putByte(this.heapPointer.value,
				obj.byteValue());
		return SIZE_OF_BYTES;
	}
	
	public long set(int i, Byte obj)
	{
		DirectMemory.getInstance().putByte(this.heapPointer.value + i,
				obj.byteValue());
		return SIZE_OF_BYTES;
	}
	
	public Byte get()
	{
		return DirectMemory.getInstance().getByte(this.heapPointer.value);
	}
	
	public Byte get(int i)
	{
		return DirectMemory.getInstance().getByte(this.heapPointer.value + (i));
	}
	
	public void dispose()
	{
		DirectMemory.getInstance().freeMemory(this.heapPointer.value);
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.dispose();
		super.finalize();
	}
	
	public long getAddress()
	{
		return this.heapPointer.value;
	}
	
	public void setAddress(long addr)
	{
		this.heapPointer.value = addr;
	}
	
	public int getSize()
	{
		return this.length;
	}
	
	public Byte pop(int i)
	{
		byte b = this.get(i);
		
		this.set(i, new Byte((byte) 0));
		
		return b;
	}
	
	public Byte lazyPop(int i)
	{
		byte b = this.get(i);
		
		this.set(i, new Byte((byte) 0));
		
		return b;
	}
}
