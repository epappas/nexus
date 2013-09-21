/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.memory.direct.DirectInt;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <V>
 */
public interface Whirl<V>
{
	static final int	NCPU		= Runtime.getRuntime()
											.availableProcessors();
	static final int	SPINS		= (NCPU == 1) ? 0 : 2000;
	static final int	TIMED_SPINS	= SPINS / 20;
	
	enum BrokerSignals
	{
		EMPTY, OK, CANCEL, VALID;
	}
	
	final Object	EMPTY	= new BucketMessage();
	
	/**
	 * @author Evangelos Pappas - Evalon.gr
	 */
	static final class Reference<V> extends AtomicReference<V>
	{
		private static final long	serialVersionUID	= 2887807675165772355L;
	}
	
	/**
	 * @author Evangelos Pappas - Evalon.gr
	 * @param <V>
	 */
	public static class BucketMessage<V>
	{
		private String					id				= "";
		private AtomicReference<V>		ref				= new AtomicReference<V>();
		private ReceiveCallback<?, V>	receiveCallback	= new ReceiveCallback<Object, V>()
														{
															public Object apply(
																	                   V v)
															{
																return null;
															}
														};
		
		public String getId()
		{
			return this.id;
		}
		
		public V getItem()
		{
			return this.ref.get();
		}
		
		public ReceiveCallback<?, V> getCallback()
		{
			return this.receiveCallback;
		}
		
		protected void setId(String id)
		{
			this.id = id;
		}
		
		protected void setItem(V item)
		{
			this.ref.set(item);
		}
		
		protected void setCallback(ReceiveCallback<?, V> callback)
		{
			this.receiveCallback = callback;
		}
	}
	
	/**
	 * @param x
	 * @return
	 * @throws InterruptedException
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public Object publish(V item);
	
	/**
	 * @param x
	 * @param receiveCallback
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public Object publish(V item, ReceiveCallback<?, V> callback);
	
	/**
	 * @param x
	 * @param receiveCallback
	 * @param expire
	 * @param timeUnit
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public Object publish(V item, ReceiveCallback<?, V> callback, long expire,
			TimeUnit timeUnit);
	
	/**
	 * @param node
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public <T extends com.evalon.chrysalis.concurrent.Request<V, V>> int subscribe(T node);
	
	/**
	 * Whirl initiate & starts its spinning logic
	 * 
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public void start();
	
	/**
	 * @return the brokerID
	 */
	public DirectInt getId();
	
	/**
	 * @return the policy
	 */
	public WhirlPolicy<V> getPolicy();
	
	/**
	 * @return the cursor
	 */
	public int getCursor();
	
	/**
	 * @return the thisNode
	 */
	public com.evalon.chrysalis.concurrent.Request<V, V> getNode();
	
	/**
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	int getMaxCapacity();
	
	/**
	 * @param item
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	Reference<BucketMessage<V>> pushBucket(BucketMessage<V> msg);
	
	/**
	 * @param nodeCrsr
	 * @param msg
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	Reference<BucketMessage<V>> pushBucketOfNode(int nodeCrsr,
			BucketMessage<V> msg);
}
