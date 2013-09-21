/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.util.Functions;

/**
 * Direct Cursor, Binary (of two) base
 * 
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectCursorBB
{
	private static final long	valueOffset;
	private int					value	= 0, max, p1, p2, p3, p4, p5, p6, p7,
			p8, p9, p10, p11;
	static
	{
		try
		{
			valueOffset = DirectMemory.getInstance().objectFieldOffset(
					DirectCursorBB.class.getDeclaredField("value"));
		}
		catch (Exception ex)
		{
			throw new Error(ex);
		}
	}
	
	public DirectCursorBB()
	{
		this(0);
	}
	
	public DirectCursorBB(final int initialValue)
	{
		this(initialValue, Integer.MAX_VALUE);
	}
	
	public DirectCursorBB(final int initialValue, final int max)
	{
		value = initialValue;
		setMax(max);
	}
	
	public boolean compareAndSet(final int expectedValue, final int newValue)
	{
		return DirectMemory.getInstance().compareAndSwapInt(this, valueOffset,
				expectedValue, newValue);
	}
	
	public void lazySet(final int newValue)
	{
		DirectMemory.getInstance().putOrderedInt(this, valueOffset, newValue);
	}
	
	public int incrementAndGet()
	{
		return addAndGet(1);
	}
	
	public int getAndIncrement()
	{
		return getAndAdd(1);
	}
	
	public int decrementAndGet()
	{
		return addAndGet(-1);
	}
	
	public int getAndDecrement()
	{
		return getAndAdd(-1);
	}
	
	public int addAndGet(int increment)
	{
		int currentValue;
		int newValue;
		
		do
		{
			currentValue = get();
			newValue = ((currentValue + increment) & this.max);
		}
		while (!compareAndSet(currentValue, newValue));
		return newValue;
	}
	
	public int getAndAdd(int increment)
	{
		int currentValue;
		int newValue;
		
		do
		{
			currentValue = get();
			newValue = ((currentValue + increment) & this.max);
		}
		while (!compareAndSet(currentValue, newValue));
		return currentValue;
	}
	
	public void setMax(int max)
	{
		this.max = Functions.nextPowerOfTwo(max) - 1;
	}
	
	public long set(int obj)
	{
		this.value = obj;
		return valueOffset;
	}
	
	public int get()
	{
		return this.value;
	}
}
