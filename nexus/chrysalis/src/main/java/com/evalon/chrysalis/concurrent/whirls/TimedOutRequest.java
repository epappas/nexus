/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface TimedOutRequest<K, V> extends com.evalon.chrysalis.concurrent.Request<K, V>
{
	/**
	 * @param message
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	K onTimedOut(V message);
}
