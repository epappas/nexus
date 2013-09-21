/**
 *
 */
package com.evalon.chrysalis.functional;


/** @author Evangelos Pappas - Evalon.gr */
public interface TraversedRequest<R, V> extends RequestCallback<R, V> {
	R apply(V v, Callback<?, V> callback);
}
