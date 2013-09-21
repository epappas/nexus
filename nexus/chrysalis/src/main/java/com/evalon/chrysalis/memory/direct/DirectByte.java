/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectByte implements Direct<Byte>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectByte(Byte val)
	{
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				SIZE_OF_BYTES);
		DirectMemory.getInstance().putByte(this.heapPointer.value,
				val.byteValue());
	}
	
	public long set(Byte obj)
	{
		DirectMemory.getInstance().putByte(this.heapPointer.value,
				obj.byteValue());
		return SIZE_OF_BYTES;
	}
	
	public Byte get()
	{
		Byte value = DirectMemory.getInstance().getByte(this.heapPointer.value);
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
		return SIZE_OF_BYTES;
	}
}
