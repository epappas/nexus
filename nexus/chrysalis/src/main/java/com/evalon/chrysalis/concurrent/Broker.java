/**
 * 
 */
package com.evalon.chrysalis.concurrent;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Broker<K, V>
{
	K publish(final com.evalon.chrysalis.concurrent.whirls.Whirl<V> whirl, final V msg);
}
