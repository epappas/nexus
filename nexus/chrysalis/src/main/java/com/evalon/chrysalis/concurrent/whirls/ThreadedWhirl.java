/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.memory.direct.DirectInt;
import com.evalon.chrysalis.memory.direct.DirectIntCursor;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <V>
 */
public class ThreadedWhirl<V> implements Whirl<V>
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
	private volatile DirectIntCursor				nodeCursor;
	private volatile DirectIntCursor				cursor;
	private DirectIntCursor[]						sequences;
	private int										lastNodeCursor;
	private com.evalon.chrysalis.concurrent.Request<V, V> thisNode;
	private final int								maxCapacity;
	private final Thread							thread;
	private final BucketMessage<V>					dummyItem;
	
	/**
	 * Creates a new Whirl.
	 */
	@SuppressWarnings("unchecked")
	public ThreadedWhirl(int id, com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes,
			int bufferCap, WhirlPolicy<V> policy)
	{
		synchronized (this)
		{
			policy.setTarget(this);
			this.policy = policy;
			this.dummyItem = this.policy.getBucketGeneration();
			this.maxCapacity = bufferCap;
			this.brokerID = new DirectInt(id <= 0 ? policy.hashIntPolicy() : id);
			this.lastNodeCursor = 0;
			this.buckets = new Reference[bufferCap];
			this.nodes = new Reference[maxNodes];
			this.sequences = new DirectIntCursor[maxNodes];
			this.cursor = new DirectIntCursor(0, bufferCap);
			this.nodeCursor = new DirectIntCursor(0, maxNodes);
			this.fillUpNodes(this.nodes);
			this.fillUpBuckets(this.buckets);
			this.fillUpSequences(this.sequences, bufferCap);
			this.setThisNode(target);
			this.subscribe(target);
			this.thread = new Thread(new SpinningExchange(), "ThreadedWhirl-"
					+ this.brokerID);
			this.thread.setDaemon(true);
		}
	}
	
	public void start()
	{
		// TODO: implement Running State
		this.thread.start();
	}
	
	private class SpinningExchange implements Runnable
	{
		private Reference<com.evalon.chrysalis.concurrent.Request<V, V>>	targetNode	= null;
		
		public void run()
		{
			for (int nodeIndex = 0; true;)
			{
				// Cache index
				nodeIndex = nodeCursor.getAndIncrement();
				// Cache Reference - makes reference
				targetNode = nodes[nodeIndex];
				try
				{
					// Signal the target about me
					// If a response has reach us
					BucketMessage<V> message = getBucketOfNode(nodeIndex).get();
					((com.evalon.chrysalis.concurrent.Request<V, V>) targetNode.get()).apply(
							                                        message.getItem(), message.getCallback());
					LockSupport.parkNanos(1);
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
	private Object doExchange(V item, ReceiveCallback<?, V> callback, long expire)
	{
		BucketMessage<V> bucketMessage = new BucketMessage<V>();
		bucketMessage.setId(this.brokerID.get() + "-" + System.nanoTime() + "-"
				+ this.cursor.get());
		bucketMessage.setItem(item);
		bucketMessage.setCallback(callback);
		return this.pushBucket(bucketMessage).get().getId();
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
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	private Reference<BucketMessage<V>> getBucketOfNode(int nodeCrsr)
	{
		return this.buckets[this.sequences[nodeCrsr].getAndIncrement()];
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
		while (true)
		{
			int i = this.sequences[nodeCrsr].get();
			if (this.buckets[i].compareAndSet(this.dummyItem, item))
			{
				this.sequences[nodeCrsr].getAndIncrement();
				return this.buckets[i];
			}
		}
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
	public Reference<BucketMessage<V>> pushBucket(BucketMessage<V> msg)
	{
		// this.buckets[this.cursor.get()].set(item);
		// return this.buckets[this.cursor.getAndIncrement()];
		// atomic CAS algorithm
		while (true)
		{
			int i = this.cursor.getAndIncrement();
			if (this.buckets[i].compareAndSet(this.dummyItem, msg))
			{
				return this.buckets[i];
			}
		}
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
