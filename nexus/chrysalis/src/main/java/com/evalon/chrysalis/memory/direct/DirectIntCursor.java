/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectIntCursor extends DirectInt implements
		DirectSequence<Integer>
{
	int	max;
	
	public DirectIntCursor()
	{
		this(-1);
	}
	
	public DirectIntCursor(final int initialValue)
	{
		this(initialValue, Integer.MAX_VALUE);
	}
	
	public DirectIntCursor(final int initialValue, final int max)
	{
		super(initialValue);
		setMax(max);
	}
	
	private long compare(final int expectedValue, final int newValue)
	{
		return (get() == expectedValue ? set(newValue) : 0L);
	}
	
	public Integer incrementAndGet()
	{
		return addAndGet(1);
	}
	
	public Integer getAndIncrement()
	{
		return getAndAdd(1);
	}
	
	public Integer addAndGet(Integer increment)
	{
		int currentValue;
		int newValue;
		
		do
		{
			currentValue = get();
			newValue = ((currentValue + increment) & Integer.MAX_VALUE) % max;//) & Integer.MAX_VALUE;
		}
		while (compare(currentValue, newValue) == 0L);
		return newValue;
	}
	
	public Integer getAndAdd(Integer increment)
	{
		int currentValue = get();
		set(((currentValue + increment) & Integer.MAX_VALUE) % max);// % max) & Integer.MAX_VALUE);
		return currentValue;
	}
	
	public void setMax(Integer max)
	{
		this.max = max;
	}
}
