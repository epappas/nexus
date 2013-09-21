/**
 * 
 */
package com.evalon.chrysalis.concurrent.loadbalance;

import com.evalon.chrysalis.concurrent.Request;
import com.evalon.chrysalis.functional.Callback;

import java.util.List;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface LoadBalancer<E extends Request<?, V>, V> extends
		Callback<E, V>
{
	/**
	 * @param block
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	E serve(V block);
	
	/**
	 * @param block
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	E serve(V block, Callback<?, V> callback);
	
	/**
	 * @param block
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	E serve(int nodeID, V block, Callback<?, V> callback);
	
	/**
	 * @param handler
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	LoadBalancer<E, V> subscribe(E handler);
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	List<E> listSubscribers();
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	int getCursor();
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	int getSize();
	
	/**
	 * @author Evangelos Pappas - Evalon.gr
	 */
	void close();
}
