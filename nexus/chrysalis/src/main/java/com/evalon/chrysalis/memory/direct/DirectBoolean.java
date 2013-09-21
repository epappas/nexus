/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectBoolean implements Direct<Boolean>
{
	private byte[]	buffer;
	
	public DirectBoolean(Boolean val)
	{
		buffer = new byte[0];
		this.set(val);
	}
	
	public long set(Boolean obj)
	{
		DirectMemory.getInstance().putBoolean(buffer, BOOLEAN_ARRAY_OFFSET,
				obj.booleanValue());
		return SIZE_OF_BOOLEAN;
	}
	
	public Boolean get()
	{
		Boolean value = DirectMemory.getInstance().getBoolean(buffer,
				BOOLEAN_ARRAY_OFFSET);
		return value;
	}
	
	public void dispose()
	{
		
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.dispose();
		super.finalize();
	}
	
	public long getAddress()
	{
		return 0;
	}
	
	public void setAddress(long addr)
	{
		
	}

	public int getSize()
	{
		return SIZE_OF_BOOLEAN;
	}
}
