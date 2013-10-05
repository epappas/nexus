/**
 * 
 */
package com.evalon.chrysalis.util;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <T>
 */
public interface Visitable<T extends Visitor<?>>
{
	public void accept(T visitor);
}
