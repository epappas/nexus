/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.concurrent.loadbalance.RoundRobinLoadBalancer;
import com.evalon.chrysalis.memory.direct.DirectInt;
import com.evalon.chrysalis.memory.pointers.Pointer;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <V>
 */
public class RoundRobinThreadedWhirl<V extends Pointer<?>> implements Whirl<V>
{
	/**
	 * Its ID
	 */
	private DirectInt								brokerID;
	
	@SuppressWarnings("rawtypes")
	private final RoundRobinLoadBalancer			balancer;
	private long									messages	= 0;
	private LinkedBlockingQueue<BucketMessage<V>>	buckets;
	private final WhirlPolicy<V>					policy;
	private final Thread							thread;
	
	// private final BucketMessage<V> dummyItem;
	
	/**
	 * Creates a new Whirl.
	 */
	public RoundRobinThreadedWhirl(int id, int bufferCap, WhirlPolicy<V> policy)
	{
		synchronized (this)
		{
			this.balancer = new RoundRobinLoadBalancer<com.evalon.chrysalis.concurrent.Request<?, V>, V>();
			policy.setTarget(this);
			this.policy = policy;
			// this.dummyItem = this.policy.getBucketGeneration();
			this.brokerID = new DirectInt(id <= 0 ? policy.hashIntPolicy() : id);
			this.buckets = new LinkedBlockingQueue<BucketMessage<V>>(bufferCap);
			this.thread = new Thread(new SpinningExchange(),
					"RoundRobinThreadedWhirl-" + this.brokerID);
			this.thread.setDaemon(true);
		}
	}
	
	public void start()
	{
		this.thread.start();
		while (!thread.isAlive())
		{
			LockSupport.parkNanos(1);
		}
	}
	
	private class SpinningExchange implements Runnable
	{
		@SuppressWarnings("unchecked")
		public void run()
		{
			while (true)
			{
				try
				{
					BucketMessage<V> message = buckets.take();
					balancer.serve(message.getItem(), message.getCallback());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				finally
				{
					
				}
			}
		}
	}
	
	/**
	 * @param item
	 * @param callback
	 * @param expire
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	private Object doExchange(V item, ReceiveCallback<?, V> callback,
			long expire)
	{
		BucketMessage<V> bucketMessage = new BucketMessage<V>();
		String id = this.brokerID.get() + "-" + System.nanoTime() + "-"
				+ (++this.messages);
		bucketMessage.setId(id);
		bucketMessage.setItem(item);
		bucketMessage.setCallback(callback);
		this.pushBucket(bucketMessage);// .get().getId();
		return id;
	}
	
	/**
	 * 
	 */
	public Object publish(V item)
	{
		return this.publish(item, null);
	}
	
	/**
	 * 
	 */
	public Object publish(V item, ReceiveCallback<?, V> callback)
	{
		return this.publish(item, callback, 0, TimeUnit.NANOSECONDS);
	}
	
	/**
	 * 
	 */
	public Object publish(V item, ReceiveCallback<?, V> callback, long expire,
			TimeUnit timeUnit)
	{
		return this.doExchange(item, callback,
				TimeUnit.NANOSECONDS.convert(expire, timeUnit));
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public <T extends com.evalon.chrysalis.concurrent.Request<V, V>> int subscribe(T node)
	{
		this.balancer.subscribe(node);
		return this.balancer.getCursor();
	}
	
	/**
	 * @param nodeCrsr
	 * @param item
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	@SuppressWarnings("unchecked")
	public Reference<BucketMessage<V>> pushBucketOfNode(int nodeCrsr,
			BucketMessage<V> item)
	{
		this.balancer.serve(nodeCrsr, item, null);
		Reference<BucketMessage<V>> reference = new Reference<BucketMessage<V>>();
		reference.set(item);
		return reference;
	}

	/**
	 *
	 * @param msg
	 * @return
	 */
	@SuppressWarnings("finally")
	public Reference<BucketMessage<V>> pushBucket(BucketMessage<V> msg)
	{
		Reference<BucketMessage<V>> reference = null;
		try
		{
			this.buckets.add(msg);
			reference = new Reference<BucketMessage<V>>();
			reference.set(msg);
		}
		finally
		{
			return reference;
		}
	}
	
	/**
	 * @return the brokerID
	 */
	public DirectInt getId()
	{
		return brokerID;
	}
	
	/**
	 * @return the policy
	 */
	public WhirlPolicy<V> getPolicy()
	{
		return policy;
	}
	
	/**
	 * @return the cursor
	 */
	public int getCursor()
	{
		return this.balancer.getCursor();
	}
	
	/**
	 * @return the thisNode
	 */
	public com.evalon.chrysalis.concurrent.Request<V, V> getNode()
	{
		return null;
	}
	
	public int getMaxCapacity()
	{
		return this.balancer.getSize();
	}
}
