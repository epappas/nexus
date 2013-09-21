/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectBytes implements Direct<byte[]>, DirectBuffer
{
	private int			length;
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectBytes(long addr, int size)
	{
		this.heapPointer.value = addr;
		this.length = size;
	}
	
	public DirectBytes(byte[] obj)
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
	
	public long set(byte[] obj)
	{
		this.length = obj.length;
		
		long bytesToCopy = this.length << 3;
		this.heapPointer.value = DirectMemory.getInstance().reallocateMemory(
				heapPointer.value, bytesToCopy);
		for (int i = 0; i < this.length; i++)
		{
			DirectMemory.getInstance().putByte(this.heapPointer.value + i,
					obj[i]);
		}
		return bytesToCopy;
	}
	
	public byte[] get()
	{
		byte[] valBArr = new byte[this.length];
		for (int i = 0; i < this.length; i++)
		{
			valBArr[i] = DirectMemory.getInstance().getByte(
					this.heapPointer.value + (i));
		}
		return valBArr;
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
	
	@Override
	public long address()
	{
		return this.heapPointer.value;
	}
	
	@Override
	public byte[] attachment()
	{
		return this.get();
	}
	
	@Override
	public Cleaner cleaner()
	{
		return null;
	}
}
