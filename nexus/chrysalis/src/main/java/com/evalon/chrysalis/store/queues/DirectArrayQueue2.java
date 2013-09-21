/**
 * 
 */
package com.evalon.chrysalis.store.queues;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.util.Functions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class DirectArrayQueue2<E> implements Queue<E>
{
	public static final int		CACHE_ALIGN	= 64;
	public static final int		BBUFFER_CAP	= CACHE_ALIGN * 4;
	// 24b,8b,32b | 24b,8b,32b | 24b,8b,32b | 24b,8b,32b
	private final ByteBuffer	bbuff;
	private final long			head;
	private final long			tailCache;
	private final long			tail;
	private final long			headCache;
	
	private final int			capacity;
	private final int			mask;
	private final E[]			items;
	private static final long	arrayBase;
	private static final int	arrayScale;
	
	static
	{
		arrayBase = DirectMemory.getInstance().arrayBaseOffset(Object[].class);
		final int scale = DirectMemory.getInstance().arrayIndexScale(
				Object[].class);
		switch (scale)
		{
			case 4:
				arrayScale = 2;
				break;
			case 8:
				arrayScale = 3;
				break;
			default:
				throw new IllegalStateException("Wrong reference size");
		}
	}
	
	@SuppressWarnings("unchecked")
	public DirectArrayQueue2(final int capacity)
	{
		// Align a block of Memory, used with padding to achieve
		// high performance for using all head, tail & caches
		bbuff = ByteBuffer.allocateDirect((int) (BBUFFER_CAP + CACHE_ALIGN));
		long alignedAddress = 0;
		try
		{
			alignedAddress = DirectMemory.getInstance().getAddressOfByteBuffer(
					bbuff);
			// Shift the position to achieve padding
			// address + (CACHE_ALIGN - (address % CACHE_ALIGN))
			int pos = (int) (CACHE_ALIGN - (alignedAddress & (CACHE_ALIGN - 1)));
			// Shift the position and capacity to the new address + padding
			bbuff.position(pos).limit(pos + BBUFFER_CAP);
			// the buffer is sliced to the new alignment & is natively ordered
			bbuff.slice().order(ByteOrder.nativeOrder());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		// addr + 24 = head
		head = alignedAddress + (CACHE_ALIGN / 2 - 8);
		// head + 64 -> padded = tailCache
		tailCache = head + CACHE_ALIGN;
		// tailCache + 64 -> padded = tail
		tail = tailCache + CACHE_ALIGN;
		// tail + 64 -> padded = headCache
		headCache = tail + CACHE_ALIGN;
		// head+tailCache+tail+headCache --> 4 Cache Lines (padding)
		
		this.capacity = Functions.nextPowerOfTwo(capacity);
		this.mask = this.capacity - 1;
		this.items = (E[]) new Object[this.capacity];
	}
	
	public boolean add(final E e)
	{
		if (offer(e))
		{
			return true;
		}
		
		throw new IllegalStateException("Queue is full");
	}
	
	public boolean offer(final E e)
	{
		if (null == e)
		{
			throw new NullPointerException("Null is not a valid element");
		}
		
		final long currentTail = getTail();
		final long wrapPoint = currentTail - capacity;
		if (getHeadCache() <= wrapPoint)
		{
			setHeadCache(getHead());
			if (getHeadCache() <= wrapPoint)
			{
				return false;
			}
		}
		
		DirectMemory.getInstance().putObject(items,
				arrayBase + ((currentTail & mask) << arrayScale), e);
		
		setTail(currentTail + 1);
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public E poll()
	{
		final long currentHead = getHead();
		if (currentHead >= getTailCache())
		{
			setTailCache(getTail());
			if (currentHead >= getTailCache())
			{
				return null;
			}
		}
		
		final long offset = arrayBase + ((currentHead & mask) << arrayScale);
		final E e = (E) DirectMemory.getInstance().getObject(items, offset);
		DirectMemory.getInstance().putObject(items, offset, null);
		setHead(currentHead + 1);
		
		return e;
	}
	
	public E remove()
	{
		final E e = poll();
		if (null == e)
		{
			throw new NoSuchElementException("Queue is empty");
		}
		
		return e;
	}
	
	public E element()
	{
		final E e = peek();
		if (null == e)
		{
			throw new NoSuchElementException("Queue is empty");
		}
		
		return e;
	}
	
	public E peek()
	{
		return items[(int) getHead() & mask];
	}
	
	public int size()
	{
		return (int) (getTail() - getHead());
	}
	
	public boolean isEmpty()
	{
		return getTail() == getHead();
	}
	
	public boolean contains(final Object o)
	{
		if (null == o)
		{
			return false;
		}
		
		for (long i = getHead(), limit = getTail(); i < limit; i++)
		{
			final E e = items[(int) i & mask];
			if (o.equals(e))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public Iterator<E> iterator()
	{
		throw new UnsupportedOperationException();
	}
	
	public Object[] toArray()
	{
		throw new UnsupportedOperationException();
	}
	
	public <T> T[] toArray(final T[] a)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean remove(final Object o)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean containsAll(final Collection<?> c)
	{
		for (final Object o : c)
		{
			if (!contains(o))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public boolean addAll(final Collection<? extends E> c)
	{
		for (final E e : c)
		{
			add(e);
		}
		
		return true;
	}
	
	public boolean removeAll(final Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean retainAll(final Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}
	
	public void clear()
	{
		Object value;
		do
		{
			value = poll();
		}
		while (null != value);
	}
	
	private long getHead()
	{
		return DirectMemory.getInstance().getLongVolatile(null, head);
	}
	
	private void setHead(final long value)
	{
		DirectMemory.getInstance().putOrderedLong(null, head, value);
	}
	
	private long getTail()
	{
		return DirectMemory.getInstance().getLongVolatile(null, tail);
	}
	
	private void setTail(final long value)
	{
		DirectMemory.getInstance().putOrderedLong(null, tail, value);
	}
	
	private long getHeadCache()
	{
		return DirectMemory.getInstance().getLong(null, headCache);
	}
	
	private void setHeadCache(final long value)
	{
		DirectMemory.getInstance().putLong(headCache, value);
	}
	
	private long getTailCache()
	{
		return DirectMemory.getInstance().getLong(null, tailCache);
	}
	
	private void setTailCache(final long value)
	{
		DirectMemory.getInstance().putLong(tailCache, value);
	}
}
