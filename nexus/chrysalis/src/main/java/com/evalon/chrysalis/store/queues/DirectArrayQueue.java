package com.evalon.chrysalis.store.queues;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.memory.direct.Direct.PaddedLong;
import com.evalon.chrysalis.memory.direct.DirectBytes;
import com.evalon.chrysalis.memory.direct.DirectCursorBB;
import com.evalon.chrysalis.util.Functions;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import static com.evalon.chrysalis.util.Functions.erase;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectArrayQueue implements Queue<byte[]>
{
	private final int				capacity;
	private final int				mask;
	private final DirectBytes[]		items;
	
	private final DirectCursorBB	tail		= new DirectCursorBB(0);
	private final DirectCursorBB	head		= new DirectCursorBB(0);
	
	private final PaddedLong		tailCache	= new PaddedLong();
	private final PaddedLong		headCache	= new PaddedLong();
	
	public DirectArrayQueue(final int capacity)
	{
		this.capacity = Functions.nextPowerOfTwo(capacity);
		mask = this.capacity - 1;
		items = new DirectBytes[this.capacity];
		
		byte[] bs = "".getBytes();
		for (int i = 0; i < items.length; ++i)
		{
			items[i] = new DirectBytes(bs);
		}
	}
	
	protected void insert(byte[] x)
	{
		final long tmpTail = tail.getAndIncrement();
		items[(int) tmpTail & mask].set(x);
	}
	
	protected byte[] extract()
	{
		int i = this.head.getAndIncrement();
		byte[] bs = this.items[i].get();
		this.items[i].dispose();
		return bs;
	}
	
	protected void removeAt(int i)
	{
		this.items[i].dispose();
	}
	
	protected byte[][] getAll()
	{
		int size = this.items.length;
		byte[][] returnedBytes = new byte[size][];
		
		for (int i = 0; i < size; ++i)
		{
			returnedBytes[i] = this.items[i].get();
		}
		
		return returnedBytes;
	}
	
	public boolean add(final byte[] e)
	{
		if (offer(e))
		{
			return true;
		}
		
		throw new IllegalStateException("Queue is full");
	}
	
	public boolean offer(final byte[] e)
	{
		final long currentTail = tail.get();
		final long wrapPoint = currentTail - capacity;
		if (headCache.value <= wrapPoint)
		{
			headCache.value = head.get();
			if (headCache.value <= wrapPoint)
			{
				return false;
			}
		}
		
		insert(e);
		return true;
	}
	
	public byte[] poll()
	{
		final long currentHead = head.get();
		if (currentHead >= tailCache.value)
		{
			tailCache.value = tail.get();
			if (currentHead >= tailCache.value)
			{
				return null;
			}
		}
		
		return this.extract();
	}
	
	public byte[] remove()
	{
		return poll();
	}
	
	public byte[] element()
	{
		return peek();
	}
	
	public byte[] peek()
	{
		return items[head.get()].get();
	}
	
	public int size()
	{
		return (int) (tail.get() - head.get());
	}
	
	public boolean isEmpty()
	{
		return tail.get() == head.get();
	}
	
	public boolean contains(final Object o)
	{
		if (null == o)
		{
			return false;
		}
		
		for (long i = head.get(), limit = tail.get(); i < limit; i++)
		{
			if (this.items[(int) i & mask].get().equals(o))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public byte[][] toArray()
	{
		return this.getAll();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] proto)
	{
		try
		{
			final T[] tmpArr = (T[]) DirectMemory.getInstance()
					.allocateInstance(proto.getClass());
			int i = 0;
			ByteArrayInputStream bis = null;
			ObjectInput in = null;
			Object tmp = null;
			for (byte[] bs : this.toArray())
			{
				bis = new ByteArrayInputStream(bs);
				in = new ObjectInputStream(bis);
				tmp = in.readObject();
				tmpArr[i] = (T) tmp;
				++i;
			}
			return null;
		}
		catch (InstantiationException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (ClassNotFoundException e)
		{
		}
		return null;
	}
	
	public String toString()
	{
		if (isEmpty())
		{
			return "[]";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (long i = head.get(), limit = tail.get(); i < limit; i++)
		{
			byte[] bs = items[(int) i & mask].get();
			sb.append("[");
			if (bs != null)
			{
				for (int j = 0; j < bs.length; ++j)
				{
					sb.append(bs[j]);
					if (j + 1 < bs.length)
					{
						sb.append(',');
					}
				}
			}
			sb.append("]");
			if (i + 1 < limit)
			{
				sb.append(',');
			}
		}
		return sb.append(']').toString();
	}
	
	public boolean remove(final Object o)
	{
		if (o == null) return false;
		final byte[][] items = this.getAll();
		for (int i = 0; i < (int) (tail.get() - head.get()); ++i)
		{
			if (o.equals(items[i]))
			{
				removeAt(i);
				return true;
			}
		}
		return false;
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
	
	public boolean addAll(Collection<? extends byte[]> collection)
	{
		for (byte[] bs : collection)
		{
			if (!this.add(bs))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean removeAll(final Collection<?> collection)
	{
		for (Object obj : collection)
		{
			if (!this.remove(obj))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean retainAll(final Collection<?> collection)
	{
		for (Object obj : collection)
		{
			if (!this.contains(obj))
			{
				if (!this.remove(obj))
				{
					return false;
				}
			}
		}
		return true;
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
	
	public boolean isFull()
	{
		final long currentTail = tail.get();
		final long wrapPoint = currentTail - capacity;
		if (headCache.value <= wrapPoint)
		{
			headCache.value = head.get();
			if (headCache.value <= wrapPoint)
			{
				return false;
			}
		}
		return true;
	}
	
	public Iterator<byte[]> iterator()
	{
		return new Iter();
	}
	
	public <T> T iterator(ReceiveCallback<T, Iterator<byte[]>> callback)
			throws InterruptedException
	{
		return callback.apply(this.iterator());
	}
	
	private class Iter implements Iterator<byte[]>
	{
		private int			remaining;
		private int			nextIndex;
		private byte[]		nextItem;
		private byte[]		lastItem;
		private int			lastRet;
		private byte[][]	iterItems;
		
		Iter()
		{
			iterItems = null;
			lastRet = -1;
			if ((remaining = (int) (tail.get() - head.get())) > 0)
			{
				iterItems = toArray();
				nextItem = (byte[]) iterItems[(nextIndex = 0)];
			}
		}
		
		public boolean hasNext()
		{
			return remaining > 0;
		}
		
		public byte[] next()
		{
			if (remaining <= 0)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			
			lastRet = nextIndex;
			byte[] x = (byte[]) iterItems[nextIndex];
			if (x == null)
			{
				x = nextItem;
				lastItem = null;
			}
			else
			{
				lastItem = x;
			}
			while (--remaining > 0
					&& (nextItem = (byte[]) (iterItems[++nextIndex])) == null)
				;
			return x;
		}
		
		public void remove()
		{
			int i = lastRet;
			if (i < 0)
			{
				throw new IllegalStateException();
			}
			
			lastRet = -1;
			byte[] x = lastItem;
			lastItem = null;
			
			if (x != null && x == iterItems[i])
			{
				boolean removingHead = (i == 0);
				erase(i, iterItems);
				--remaining;
				if (!removingHead)
				{
					--nextIndex;
				}
			}
		}
	}
}
