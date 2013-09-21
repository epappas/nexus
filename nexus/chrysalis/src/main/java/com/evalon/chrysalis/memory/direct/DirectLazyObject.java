/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectLazyObject<K extends Object> extends DirectObject<K>
		implements DirectLazy<K>
{
	private ExecutorService	executor;
	
	public DirectLazyObject(K obj) throws IOException
	{
		super(obj);
		executor = Executors.newSingleThreadExecutor();
	}
	
	public K lazySet(final K obj) throws InterruptedException,
			ExecutionException
	{
		return this.executor.submit(new Callable<K>()
		{
			public K call() throws Exception
			{
				set(obj);
				return obj;
			}
		}).get();
	}
	
	public K lazyGet() throws InterruptedException, ExecutionException
	{
		return this.executor.submit(new Callable<K>()
		{
			public K call() throws Exception
			{
				return get();
			}
		}).get();
	}
	
	public K lazySet(final K obj, long timeOut) throws InterruptedException,
			ExecutionException, TimeoutException
	{
		return this.executor.submit(new Callable<K>()
		{
			public K call() throws Exception
			{
				set(obj);
				return obj;
			}
		}).get(timeOut, TimeUnit.NANOSECONDS);
	}
	
	public K lazyGet(long timeOut) throws InterruptedException,
			ExecutionException, TimeoutException
	{
		return this.executor.submit(new Callable<K>()
		{
			public K call() throws Exception
			{
				return get();
			}
		}).get(timeOut, TimeUnit.NANOSECONDS);
	}
	
}
