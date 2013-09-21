/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectDouble implements Direct<Double>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectDouble(Double val)
	{
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				SIZE_OF_DOUBLE);
		DirectMemory.getInstance().putDouble(this.heapPointer.value,
				val.doubleValue());
	}
	
	public long set(Double obj)
	{
		DirectMemory.getInstance().putDouble(this.heapPointer.value,
				obj.doubleValue());
		return SIZE_OF_DOUBLE;
	}
	
	public Double get()
	{
		Double value = DirectMemory.getInstance().getDouble(this.heapPointer.value);
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
		return SIZE_OF_DOUBLE;
	}
}
