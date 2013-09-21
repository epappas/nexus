/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface DirectArray<T> extends Direct<T>
{
	long set(T[] obj);
	
	long set(int i, T obj);
	
	T get(int i);

	T pop(int i);
	
	T lazyPop(int i);
}
