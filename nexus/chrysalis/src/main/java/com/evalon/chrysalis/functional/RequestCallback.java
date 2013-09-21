/**
 *
 */
package com.evalon.chrysalis.functional;


/** @author Evangelos Pappas - Evalon.gr */
public interface RequestCallback<R, V> extends Callback<R, V> {
	/**
	 * @param token
	 *
	 * @return
	 *
	 * @author Evangelos Pappas - Evalon.gr
	 */
	R apply(V token);
}
