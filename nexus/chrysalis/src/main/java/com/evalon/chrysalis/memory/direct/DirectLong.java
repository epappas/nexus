/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectLong implements Direct<Long>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectLong(Long val)
	{
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(SIZE_OF_LONG);
		DirectMemory.getInstance().putLong(this.heapPointer.value, val.longValue());
	}
	
	public long set(Long obj)
	{
		DirectMemory.getInstance().putLong(this.heapPointer.value, obj.longValue());
		return SIZE_OF_LONG;
	}
	
	public Long get()
	{
		return DirectMemory.getInstance().getLong(this.heapPointer.value);
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
		return SIZE_OF_LONG;
	}
}
