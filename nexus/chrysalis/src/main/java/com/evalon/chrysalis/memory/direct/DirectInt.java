/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectInt implements Direct<Integer>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectInt(int val)
	{
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				SIZE_OF_INT);
		DirectMemory.getInstance().putInt(this.heapPointer.value, val);
	}
	
	public long set(Integer obj)
	{
		DirectMemory.getInstance().putInt(this.heapPointer.value,
				obj.intValue());
		return SIZE_OF_INT;
	}
		
	public Integer get()
	{
		return DirectMemory.getInstance().getInt(this.heapPointer.value);
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
		return SIZE_OF_INT;
	}
}
