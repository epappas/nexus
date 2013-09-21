/**
 * 
 */
package com.evalon.chrysalis.concurrent.sync;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Mutex
{
	public void acquire() throws InterruptedException;
	
	public boolean attempt(long msecs) throws InterruptedException;
	
	public void release();
	
}
