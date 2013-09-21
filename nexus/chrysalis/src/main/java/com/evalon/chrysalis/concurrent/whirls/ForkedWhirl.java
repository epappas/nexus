/**
 * 
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.memory.direct.DirectInt;
import com.evalon.chrysalis.memory.direct.DirectIntCursor;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <V>
 */
public class ForkedWhirl<V> implements Whirl<V>
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
	
	/**
	 * Creates a new Whirl.
	 */
	@SuppressWarnings("unchecked")
	public ForkedWhirl(int id, com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes,
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
	
	private class SpinningTask extends ForkJoinTask<V>
	{
		private static final long				serialVersionUID	= 1706624144649503721L;
		private final Reference<com.evalon.chrysalis.concurrent.Request<V, V>>	targetNode;
		private final int						nodeIndex;
		
		public SpinningTask(int index)
		{
			super();
			this.nodeIndex = index;
			this.targetNode = nodes[this.nodeIndex];
		}
		
		@Override
		protected boolean exec()
		{
			BucketMessage<V> message = getBucketOfNode(nodeIndex).get();
			((com.evalon.chrysalis.concurrent.Request<V, V>) this.targetNode.get()).apply(
					                                             message.getItem(), message.getCallback());
			return true;
		}
		
		@Override
		public V getRawResult()
		{
			return null;
		}
		
		@Override
		protected void setRawResult(V value)
		{
			
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
		
		Object ptr = this.pushBucket(bucketMessage).get().getId();
		int length = nodes.length;
		for (int i = 0; i < length; ++i)
		{
			// Execute each callback in a forked Thread
			(new SpinningTask(i)).fork();
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
		for (int i = 0; i < slots.length - 1; ++i)
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
