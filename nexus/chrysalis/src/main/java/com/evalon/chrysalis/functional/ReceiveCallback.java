package com.evalon.chrysalis.functional;


/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface ReceiveCallback<R, V> extends Callback<R, V>
{
	R apply(V v);
}
