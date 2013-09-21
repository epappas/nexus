/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectShort implements Direct<Short>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectShort(Short val)
	{
		this.set(val);
	}
	
	public long set(Short obj)
	{
		this.dispose();
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				SIZE_OF_SHORT);
		DirectMemory.getInstance().putShort(this.heapPointer.value,
				obj.shortValue());
		return SIZE_OF_SHORT;
	}
	
	public Short get()
	{
		Short value = DirectMemory.getInstance().getShort(
				this.heapPointer.value);
		return value;
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
		return SIZE_OF_SHORT;
	}
}
