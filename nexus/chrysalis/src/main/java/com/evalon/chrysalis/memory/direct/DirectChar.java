/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectChar implements Direct<Character>
{
	private PaddedLong	heapPointer	= new PaddedLong();
	
	public DirectChar(Character val)
	{
		this.heapPointer.value = DirectMemory.getInstance().allocateMemory(
				SIZE_OF_CHAR);
		DirectMemory.getInstance().putChar(this.heapPointer.value,
				val.charValue());
	}
	
	public long set(Character obj)
	{
		DirectMemory.getInstance().putChar(this.heapPointer.value,
				obj.charValue());
		return SIZE_OF_CHAR;
	}
	
	public Character get()
	{
		Character value = DirectMemory.getInstance().getChar(
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
		return SIZE_OF_CHAR;
	}
}
