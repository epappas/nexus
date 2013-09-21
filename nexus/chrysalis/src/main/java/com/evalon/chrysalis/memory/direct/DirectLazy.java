/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface DirectLazy<T> extends Direct<T>
{
	T lazySet(T obj) throws InterruptedException, ExecutionException;
	
	T lazySet(T obj, long timeOut) throws InterruptedException,
			ExecutionException, TimeoutException;
	
	T lazyGet() throws InterruptedException, ExecutionException;
	
	T lazyGet(long timeOut) throws InterruptedException, ExecutionException,
			TimeoutException;
}
