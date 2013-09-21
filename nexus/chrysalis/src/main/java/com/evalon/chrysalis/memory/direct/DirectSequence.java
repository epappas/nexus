/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface DirectSequence<T> extends Direct<T>
{
	public T incrementAndGet();
	
	public T getAndIncrement();
	
	public T addAndGet(T increment);
	
	public T getAndAdd(T increment);
	
	public void setMax(T max);
}
