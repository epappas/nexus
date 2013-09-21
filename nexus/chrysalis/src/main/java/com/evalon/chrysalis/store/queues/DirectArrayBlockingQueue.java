package com.evalon.chrysalis.store.queues;

import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectArrayBlockingQueue implements Queue<byte[]>
{
	private final DirectArrayQueue	queue;
	
	final ReentrantLock				lock;
	private final Condition			isEmpty;
	private final Condition			isFull;
	
	public DirectArrayBlockingQueue(int capacity)
	{
		this(capacity, false);
	}
	
	/**
	 * @param capacity
	 * @param fair
	 */
	public DirectArrayBlockingQueue(int capacity, boolean fair)
	{
		this.queue = new DirectArrayQueue(capacity);
		this.lock = new ReentrantLock(fair);
		this.isEmpty = lock.newCondition();
		this.isFull = lock.newCondition();
	}
	
	/**
	 * @param capacity
	 * @param fair
	 * @param c
	 */
	public DirectArrayBlockingQueue(int capacity, boolean fair,
			Collection<byte[]> c)
	{
		this(capacity, fair);
		
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			this.queue.addAll(c);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private void insert(byte[] x)
	{
		this.queue.insert(x);
		isEmpty.signal();
	}
	
	private byte[] extract()
	{
		byte[] x = this.queue.extract();
		isFull.signal();
		return x;
	}
	
	public boolean add(byte[] e)
	{
		if (offer(e))
		{
			return true;
		}
		
		throw new IllegalStateException("Queue full");
	}
	
	public <T> T add(byte[] e, ReceiveCallback<T, Boolean> callback)
			throws InterruptedException
	{
		return callback.apply(this.add(e));
	}
	
	public boolean addAll(Collection<? extends byte[]> collection)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.addAll(collection);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T addAll(Collection<? extends byte[]> collection,
			ReceiveCallback<T, Boolean> callback) throws InterruptedException
	{
		return callback.apply(this.addAll(collection));
	}
	
	public boolean offer(byte[] e)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.offer(e);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T offer(byte[] e, ReceiveCallback<T, Boolean> callback)
			throws InterruptedException
	{
		return callback.apply(this.offer(e));
	}
	
	public void put(byte[] e) throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while (this.queue.isFull())
			{
				isFull.await();
			}
			insert(e);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean offer(byte[] e, long timeout, TimeUnit unit)
			throws InterruptedException
	{
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while (this.queue.isFull())
			{
				if (nanos <= 0)
				{
					return false;
				}
				nanos = isFull.awaitNanos(nanos);
			}
			insert(e);
			return true;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T offer(byte[] e, ReceiveCallback<T, Boolean> callback,
			long timeout, TimeUnit unit) throws InterruptedException
	{
		return callback.apply(this.offer(e, timeout, unit));
	}
	
	public byte[] poll()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return (this.queue.isEmpty() ? null : extract());
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T poll(ReceiveCallback<T, byte[]> callback)
			throws InterruptedException
	{
		return callback.apply(this.poll());
	}
	
	public byte[] take() throws InterruptedException
	{
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while (this.queue.isEmpty())
			{
				isEmpty.await();
			}
			return extract();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T take(ReceiveCallback<T, byte[]> callback)
			throws InterruptedException
	{
		return callback.apply(this.take());
	}
	
	public byte[] poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while (this.queue.isEmpty())
			{
				if (nanos <= 0)
				{
					return null;
				}
				nanos = isEmpty.awaitNanos(nanos);
			}
			return extract();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T poll(ReceiveCallback<T, byte[]> callback, long timeout,
			TimeUnit unit) throws InterruptedException
	{
		return callback.apply(this.poll(timeout, unit));
	}
	
	public byte[] peek()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return (this.queue.isEmpty() ? null : this.queue.peek());
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T peek(ReceiveCallback<T, byte[]> callback)
			throws InterruptedException
	{
		return callback.apply(this.peek());
	}
	
	public byte[] element()
	{
		return this.peek();
	}
	
	public <T> T element(ReceiveCallback<T, byte[]> callback)
			throws InterruptedException
	{
		return callback.apply(this.element());
	}
	
	public int size()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.size();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public byte[] remove()
	{
		return this.extract();
	}
	
	public <T> T remove(ReceiveCallback<T, byte[]> callback)
			throws InterruptedException
	{
		return callback.apply(this.remove());
	}
	
	public boolean remove(Object o)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.remove(o);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean removeAll(Collection<?> collection)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.removeAll(collection);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean retainAll(Collection<?> collection)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.retainAll(collection);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean contains(Object obj)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.contains(obj);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean containsAll(Collection<?> collection)
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.containsAll(collection);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public byte[][] toArray()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.toArray();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T toArray(ReceiveCallback<T, byte[][]> callback)
			throws InterruptedException
	{
		return callback.apply(this.toArray());
	}
	
	public <T> T[] toArray(T[] proto)
	{
		return this.queue.toArray(proto);
	}
	
	public <T, Z> T toArray(Z[] proto, ReceiveCallback<T, Z[]> callback)
			throws InterruptedException
	{
		return callback.apply(this.toArray(proto));
	}
	
	public String toString()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return this.queue.toString();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public <T> T toString(ReceiveCallback<T, String> callback)
			throws InterruptedException
	{
		return callback.apply(this.toString());
	}
	
	public void clear()
	{
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			this.queue.clear();
			isFull.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public boolean isEmpty()
	{
		return this.queue.isEmpty();
	}
	
	public Iterator<byte[]> iterator()
	{
		return this.queue.iterator();
	}
	
	public <T> T iterator(ReceiveCallback<T, Iterator<byte[]>> callback)
			throws InterruptedException
	{
		return callback.apply(this.iterator());
	}
}
