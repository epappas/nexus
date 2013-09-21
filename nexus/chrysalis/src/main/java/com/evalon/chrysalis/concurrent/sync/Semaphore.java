/**
 * 
 */
package com.evalon.chrysalis.concurrent.sync;

import com.evalon.chrysalis.memory.direct.Direct.PaddedLong;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class Semaphore implements Mutex
{
	private PaddedLong			accedence	= new PaddedLong();
	private final Queue<Thread>	waiters;
	
	public Semaphore(long initialPermits)
	{
		accedence.value = initialPermits;
		waiters = new ConcurrentLinkedQueue<Thread>();
	}
	
	public void acquire() throws InterruptedException
	{
		Thread current = Thread.currentThread();
		this.waiters.add(current);
		
		while (true)
		{
			if (accedence.value > 0)
			{
				if (waiters.peek() == current)
				{
					--accedence.value;
					waiters.remove();
					if (Thread.interrupted())
					{
						current.interrupt();
					}
					return;
				}
			}
			else
			{
				LockSupport.park(this);
			}
		}
	}
	
	public boolean attempt(long nsecs) throws InterruptedException
	{
		Thread current = Thread.currentThread();
		this.waiters.add(current);
		
		long startTime = System.nanoTime();
		long waitTime = nsecs;
		try
		{
			do
			{
				LockSupport.parkNanos(this, waitTime);
				waitTime = nsecs - (System.nanoTime() - startTime);
			}
			while ((waiters.peek() != current || accedence.value <= 0)
					&& waitTime > 0);
			
			if (accedence.value > 0 && waiters.peek() == current)
			{
				--accedence.value;
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			waiters.remove(current);
			if (Thread.interrupted())
			{
				current.interrupt();
			}
		}
	}
	
	public void release()
	{
		++accedence.value;
		LockSupport.unpark(waiters.peek());
	}
	
	public void release(long n)
	{
		accedence.value += n;
		for (long i = 0; i < n; ++i)
		{
			LockSupport.unpark(waiters.peek());
		}
	}
	
	public long grants()
	{
		return accedence.value;
	}
}
