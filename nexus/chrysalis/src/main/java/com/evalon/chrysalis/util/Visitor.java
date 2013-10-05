package com.evalon.chrysalis.util;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <T>
 */
public interface Visitor<T extends Visitable<?>>
{
	public void visit(T visitable);
}
