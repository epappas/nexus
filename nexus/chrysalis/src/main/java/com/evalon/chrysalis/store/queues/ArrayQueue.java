package com.evalon.chrysalis.store.queues;

import com.evalon.chrysalis.memory.direct.Direct.PaddedLong;
import com.evalon.chrysalis.memory.direct.DirectCursorBB;
import com.evalon.chrysalis.util.Functions;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import static com.evalon.chrysalis.util.Functions.erase;

/** @author Evangelos Pappas - Evalon.gr */
public class ArrayQueue<E> implements Queue<E> {
	private final int capacity;
	private final int mask;
	private final E[] items;

	private final DirectCursorBB tail = new DirectCursorBB(0);
	private final DirectCursorBB head = new DirectCursorBB(0);

	private final PaddedLong tailCache = new PaddedLong();
	private final PaddedLong headCache = new PaddedLong();

	@SuppressWarnings("unchecked")
	public ArrayQueue(final int capacity) {
		this.capacity = Functions.nextPowerOfTwo(capacity);
		mask = this.capacity - 1;
		items = (E[]) new Object[this.capacity];
	}

	protected void insert(E x) {
		// final int i = (int) tail.incrementAndGet() & mask;
		items[tail.incrementAndGet() & mask] = x;
	}

	protected E extract() {
		final int i = (int) this.head.incrementAndGet() & mask;
		E e = this.items[i];
		this.items[i] = null;
		return e;
	}

	protected void removeAt(int i) {
		this.items[i & mask] = null;
	}

	@SuppressWarnings("unchecked")
	protected E[] getAll() {
		int size = this.items.length;
		E[] es = (E[]) new Object[size];

		for (int i = 0; i < size; ++i) {
			es[i] = this.items[i];
		}

		return es;
	}

	public boolean add(final E e) {
		if (offer(e)) {
			return true;
		}

		throw new IllegalStateException("Queue is full");
	}

	public boolean offer(final E e) {
		final long wrapPoint = tail.get() - capacity;
		if (headCache.value <= wrapPoint) {
			headCache.value = head.get();
			if (headCache.value <= wrapPoint) {
				return false;
			}
		}

		insert(e);
		return true;
	}

	public E poll() {
		final long currentHead = head.get();
		if (currentHead >= tailCache.value) {
			tailCache.value = tail.get();
			if (currentHead >= tailCache.value) {
				return null;
			}
		}

		return this.extract();
	}

	public E remove() {
		return poll();
	}

	public E element() {
		return peek();
	}

	public E peek() {
		return items[(int) head.get() & mask];
	}

	public int size() {
		return (int) (tail.get() - head.get());
	}

	public boolean isEmpty() {
		return tail.get() == head.get();
	}

	public boolean contains(final Object o) {
		if (null == o) {
			return false;
		}

		for (long i = head.get(), limit = tail.get(); i < limit; i++) {
			if (this.items[(int) i & mask].equals(o)) {
				return true;
			}
		}

		return false;
	}

	public E[] toArray() {
		return this.getAll();
	}

	public <T> T[] toArray(final T[] proto) {
		throw new UnsupportedOperationException(
				                                       "toArray is not yet implemented");
	}

	public boolean remove(final Object o) {
		if (o == null) return false;
		final E[] items = this.getAll();
		for (int i = 0; i < (int) (tail.get() - head.get()); ++i) {
			if (o.equals(items[i])) {
				removeAt(i);
				return true;
			}
		}
		return false;
	}

	public boolean containsAll(final Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}

		return true;
	}

	public boolean addAll(Collection<? extends E> collection) {
		for (E bs : collection) {
			if (!this.add(bs)) {
				return false;
			}
		}
		return true;
	}

	public boolean removeAll(final Collection<?> collection) {
		for (Object obj : collection) {
			if (!this.remove(obj)) {
				return false;
			}
		}
		return true;
	}

	public boolean retainAll(final Collection<?> collection) {
		for (Object obj : collection) {
			if (!this.contains(obj)) {
				if (!this.remove(obj)) {
					return false;
				}
			}
		}
		return true;
	}

	public void clear() {
		Object value;
		do {
			value = poll();
		}
		while (null != value);
	}

	public boolean isFull() {
		final long currentTail = tail.get();
		final long wrapPoint = currentTail - capacity;
		if (headCache.value <= wrapPoint) {
			headCache.value = head.get();
			if (headCache.value <= wrapPoint) {
				return false;
			}
		}
		return true;
	}

	public Iterator<E> iterator() {
		return new Iter();
	}

	public <T> T iterator(ReceiveCallback<T, Iterator<E>> callback)
			throws InterruptedException {
		return callback.apply(this.iterator());
	}

	private class Iter implements Iterator<E> {
		private int remaining;
		private int nextIndex;
		private E nextItem;
		private E lastItem;
		private int lastRet;
		private E[] iterItems;

		Iter() {
			iterItems = null;
			lastRet = -1;
			if ((remaining = (int) (tail.get() - head.get())) > 0) {
				iterItems = toArray();
				nextItem = (E) iterItems[(nextIndex = 0)];
			}
		}

		public boolean hasNext() {
			return remaining > 0;
		}

		public E next() {
			if (remaining <= 0) {
				throw new ArrayIndexOutOfBoundsException();
			}

			lastRet = nextIndex;
			E x = (E) iterItems[nextIndex];
			if (x == null) {
				x = nextItem;
				lastItem = null;
			} else {
				lastItem = x;
			}
			// Spin
			while (--remaining > 0
					       && (nextItem = (E) (iterItems[++nextIndex])) == null) { ; }
			return x;
		}

		public void remove() {
			int i = lastRet;
			if (i < 0) {
				throw new IllegalStateException();
			}

			lastRet = -1;
			E x = lastItem;
			lastItem = null;

			if (x != null && x == iterItems[i]) {
				boolean removingHead = (i == 0);
				erase(i, iterItems);
				--remaining;
				if (!removingHead) {
					--nextIndex;
				}
			}
		}
	}
}
