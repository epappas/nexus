/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectLongArray implements DirectArray<Long>
{
	private int			length;
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectLongArray(long[] obj)
	{
		this.length = obj.length;
		
		long bytesToCopy = length << 3;
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				bytesToCopy);
		for (int i = 0; i < this.length; i++)
		{
			DirectMemory.getInstance().putLong(this.heapPointer.value + i,
					obj[i]);
		}
	}
	
	public long set(Long[] obj)
	{
		this.length = obj.length;
		
		long bytesToCopy = length << 3;
		this.heapPointer.value = DirectMemory.getInstance().reallocateMemory(
				this.heapPointer.value, bytesToCopy);
		for (int i = 0; i < this.length; i++)
		{
			DirectMemory.getInstance().putLong(this.heapPointer.value + i,
					obj[i]);
		}
		return bytesToCopy;
	}
	
	public long set(Long obj)
	{
		DirectMemory.getInstance().putLong(this.heapPointer.value,
				obj.longValue());
		return SIZE_OF_LONG;
	}
	
	public long set(int i, Long obj)
	{
		DirectMemory.getInstance().putLong(this.heapPointer.value + i,
				obj.longValue());
		return SIZE_OF_LONG;
	}
	
	public Long get()
	{
		return DirectMemory.getInstance().getLong(this.heapPointer.value);
	}
	
	public Long get(int i)
	{
		return DirectMemory.getInstance().getLong(this.heapPointer.value + (i));
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
		return length * SIZE_OF_LONG;
	}
	
	public Long pop(int i)
	{
		long l = this.get(i);
		
		this.set(i, new Long(0L));
		
		return l;
	}
	
	public Long lazyPop(int i)
	{
		long l = this.get(i);
		
		this.set(i, new Long(0L));
		
		return l;
	}
}
