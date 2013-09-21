/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectFloat implements Direct<Float>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectFloat(Float val)
	{
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(SIZE_OF_FLOAT);
		DirectMemory.getInstance().putFloat(this.heapPointer.value, val.floatValue());
	}
	
	public long set(Float obj)
	{
		DirectMemory.getInstance().putFloat(this.heapPointer.value, obj.floatValue());
		return SIZE_OF_FLOAT;
	}
	
	public Float get()
	{
		Float value = DirectMemory.getInstance().getFloat(this.heapPointer.value);
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
		return SIZE_OF_FLOAT;
	}
}
