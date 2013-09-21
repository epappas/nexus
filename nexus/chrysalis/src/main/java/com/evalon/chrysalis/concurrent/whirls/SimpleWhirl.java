/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.memory.direct.DirectInt;
import com.evalon.chrysalis.memory.direct.DirectIntCursor;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <V>
 */
public class SimpleWhirl<V> implements Whirl<V>
{
	/**
	 * It's ID
	 */
	private DirectInt								brokerID;
	/**
	 * 
	 */
	private volatile Reference<com.evalon.chrysalis.concurrent.Request<V, V>>[]		nodes;
	private volatile Reference<BucketMessage<V>>[]	buckets;
	private final WhirlPolicy<V>					policy;
	private volatile DirectIntCursor				cursor;
	private DirectIntCursor[]						sequences;
	private int										lastNodeCursor;
	private com.evalon.chrysalis.concurrent.Request<V, V> thisNode;
	private final int								maxCapacity;
	private final Lock								lock	= new ReentrantLock();
	
	/**
	 * Creates a new Whirl.
	 */
	@SuppressWarnings("unchecked")
	public SimpleWhirl(int id, com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes,
			int bufferCap, WhirlPolicy<V> policy)
	{
		synchronized (this)
		{
			policy.setTarget(this);
			this.policy = policy;
			this.maxCapacity = bufferCap;
			this.brokerID = new DirectInt(id <= 0 ? policy.hashIntPolicy() : id);
			this.lastNodeCursor = 0;
			this.buckets = new Reference[bufferCap];
			this.nodes = new Reference[maxNodes];
			this.sequences = new DirectIntCursor[maxNodes];
			this.cursor = new DirectIntCursor(0, bufferCap);
			this.fillUpNodes(this.nodes);
			this.fillUpBuckets(this.buckets);
			this.fillUpSequences(this.sequences, bufferCap);
			this.setThisNode(target);
			this.subscribe(target);
		}
	}
	
	public void start()
	{
		// TODO: implement Running State
	}
	
	/**
	 * @param item
	 * @param callback
	 * @param expire
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	private Object doExchange(V item, ReceiveCallback<?, V> callback, long expire)
	{
		BucketMessage<V> bucketMessage = new BucketMessage<V>();
		bucketMessage.setId(this.brokerID.get() + "-" + System.nanoTime() + "-"
				+ this.cursor.get());
		bucketMessage.setItem(item);
		bucketMessage.setCallback(callback);
		Object ptr = this.pushBucket(bucketMessage).get().getId();
		
		try
		{
			while (true)
			{
				if (this.lock.tryLock())
				{
					for (Reference<com.evalon.chrysalis.concurrent.Request<V, V>> reference : this.nodes)
					{
						((com.evalon.chrysalis.concurrent.Request<V, V>) reference.get()).apply(
								                                       bucketMessage.getItem(),
								                                       bucketMessage.getCallback());
					}
					break;
				}
			}
		}
		finally
		{
			this.lock.unlock();
		}
		
		return ptr;
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
	public <T extends com.evalon.chrysalis.concurrent.Request<V, V>> int subscribe(T node)
	{
		nodes[lastNodeCursor] = new Reference<com.evalon.chrysalis.concurrent.Request<V, V>>();
		nodes[lastNodeCursor].set((com.evalon.chrysalis.concurrent.Request<V, V>) node);
		return lastNodeCursor++;
	}
	
	/**
	 * @param nodeCrsr
	 * @param item
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public Reference<BucketMessage<V>> pushBucketOfNode(int nodeCrsr,
			BucketMessage<V> item)
	{
		this.buckets[this.sequences[nodeCrsr].get()].set(item);
		return this.buckets[this.sequences[nodeCrsr].getAndIncrement()];
	}
	
	/**
	 * @param i
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	@SuppressWarnings("unused")
	private Reference<BucketMessage<V>> getBucket(int i)
	{
		return this.buckets[i];
	}
	
	/**
	 * @param item
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public Reference<BucketMessage<V>> pushBucket(BucketMessage<V> item)
	{
		this.buckets[this.cursor.get()].set(item);
		return this.buckets[this.cursor.getAndIncrement()];
	}
	
	private void fillUpNodes(Reference<com.evalon.chrysalis.concurrent.Request<V, V>>[] slots)
	{
		Reference<com.evalon.chrysalis.concurrent.Request<V, V>>[] a = slots;
		for (int i = 0; i < slots.length; ++i)
		{
			synchronized (a)
			{
				if (a[i] == null)
				{
					a[i] = new Reference<com.evalon.chrysalis.concurrent.Request<V, V>>();
					a[i].set((com.evalon.chrysalis.concurrent.Request<V, V>) this.policy.getNodeGeneration());
				}
			}
		}
	}
	
	private void fillUpBuckets(Reference<BucketMessage<V>>[] slots)
	{
		Reference<BucketMessage<V>>[] a = slots;
		for (int i = 0; i < slots.length; ++i)
		{
			synchronized (a)
			{
				if (a[i] == null)
				{
					a[i] = new Reference<BucketMessage<V>>();
					a[i].set(this.policy.getBucketGeneration());
				}
			}
		}
	}
	
	private void fillUpSequences(DirectIntCursor[] seq, int bufferCap)
	{
		DirectIntCursor[] a = seq;
		for (int i = 0; i < seq.length; ++i)
		{
			synchronized (a)
			{
				if (a[i] == null) a[i] = new DirectIntCursor(0, bufferCap);
			}
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
		return this.cursor.get();
	}
	
	/**
	 * @return the thisNode
	 */
	public com.evalon.chrysalis.concurrent.Request<V, V> getNode()
	{
		return thisNode;
	}
	
	/**
	 * @param thisNode
	 *            the thisNode to set
	 */
	protected void setThisNode(com.evalon.chrysalis.concurrent.Request<V, V> thisNode)
	{
		this.thisNode = thisNode;
	}
	
	/**
	 * @return the maxCapacity
	 */
	public int getMaxCapacity()
	{
		return maxCapacity;
	}
}
