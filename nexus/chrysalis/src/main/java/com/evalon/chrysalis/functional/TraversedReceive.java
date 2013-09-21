/**
 *
 */
package com.evalon.chrysalis.functional;


/** @author Evangelos Pappas - Evalon.gr */
public interface TraversedReceive<R, V> extends ReceiveCallback<R, V> {
	R apply(V v, Callback<?, V> callback);
}
