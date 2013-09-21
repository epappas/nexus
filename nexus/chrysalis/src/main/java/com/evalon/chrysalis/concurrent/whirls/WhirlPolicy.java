/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.concurrent.whirls.Whirl.BucketMessage;
import com.evalon.chrysalis.concurrent.whirls.Whirl.Reference;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface WhirlPolicy<V>
{
	/**
	 * @param reference
	 * @author Evangelos Pappas - Evalon.gr
	 */
	<T extends com.evalon.chrysalis.concurrent.Request<V, V>> void unsubscribe(Reference<T> reference);
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	BucketMessage<V> getBucketGeneration();
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	<T extends com.evalon.chrysalis.concurrent.Request<V, V>> T getNodeGeneration();
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	Whirl<V> getTarget();
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	void setTarget(Whirl<V> broker);
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	int hashIntPolicy();
}
