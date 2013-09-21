package com.evalon.chrysalis.store.chm;

import com.evalon.chrysalis.memory.DirectMemory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <K>
 * @param <V>
 */
public class DirectConcurrentHashMap<K, V> implements ConcurrentMap<K, V>,
		Serializable
{
	private static final long		serialVersionUID			= 7249069246763182397L;
	
	/**
	 * The default initial capacity for this table, used when not otherwise
	 * specified in a constructor.
	 */
	static final int				DEFAULT_INITIAL_CAPACITY	= 16;
	
	/**
	 * The default load factor for this table, used when not otherwise specified
	 * in a constructor.
	 */
	static final float				DEFAULT_LOAD_FACTOR			= 0.75f;
	
	/**
	 * The default concurrency level for this table, used when not otherwise
	 * specified in a constructor.
	 */
	static final int				DEFAULT_CONCURRENCY_LEVEL	= 16;
	
	/**
	 * The maximum capacity, used if a higher value is implicitly specified by
	 * either of the constructors with arguments. MUST be a power of two <=
	 * 1<<30 to ensure that entries are indexable using ints.
	 */
	static final int				MAXIMUM_CAPACITY			= 1 << 30;
	
	/**
	 * The minimum capacity for per-segment tables. Must be a power of two, at
	 * least two to avoid immediate resizing on next use after lazy
	 * construction.
	 */
	static final int				MIN_SEGMENT_TABLE_CAPACITY	= 2;
	
	/**
	 * The maximum number of segments to allow; used to bound constructor
	 * arguments. Must be power of two less than 1 << 24.
	 */
	static final int				MAX_SEGMENTS				= 1 << 16;				// slightly
																						// conservative
																						
	/**
	 * Number of unsynchronized retries in size and containsValue methods before
	 * resorting to locking. This is used to avoid unbounded retries if tables
	 * undergo continuous modification which would make it impossible to obtain
	 * an accurate result.
	 */
	static final int				RETRIES_BEFORE_LOCK			= 2;
	
	/* ---------------- Fields -------------- */
	
	/**
	 * Mask value for indexing into segments. The upper bits of a key's hash
	 * code are used to choose the segment.
	 */
	final int						segmentMask;
	
	/**
	 * Shift value for indexing within segments.
	 */
	final int						segmentShift;
	
	/**
	 * The segments, each of which is a specialized hash table.
	 */
	final Segment<K, V>[]			segments;
	
	transient Set<K>				keySet;
	transient Set<Map.Entry<K, V>>	entrySet;
	transient Collection<V>			values;
	
	/**
	 * DirectConcurrentHashMap list entry. Note that this is never exported out
	 * as a user-visible Map.Entry.
	 */
	static final class HashEntry<K, V>
	{
		final int					hash;
		final K						key;
		volatile V					value;
		volatile HashEntry<K, V>	next;
		static final long			nextOffset;
		static
		{
			try
			{
				final Class k = HashEntry.class;
				nextOffset = DirectMemory.getInstance().objectFieldOffset(
						k.getDeclaredField("next"));
			}
			catch (final Exception e)
			{
				throw new Error(e);
			}
		}
		
		HashEntry(final int hash, final K key, final V value,
				final HashEntry<K, V> next)
		{
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}
		
		/**
		 * Sets next field with volatile write semantics. (See above about use
		 * of putOrderedObject.)
		 */
		final void setNext(final HashEntry<K, V> n)
		{
			DirectMemory.getInstance().putOrderedObject(this,
					HashEntry.nextOffset, n);
		}
	}
	
	/**
	 * Gets the ith element of given table (if nonnull) with volatile read
	 * semantics. Note: This is manually integrated into a few
	 * performance-sensitive methods to reduce call overhead.
	 */
	@SuppressWarnings("unchecked")
	static final <K, V> HashEntry<K, V> entryAt(final HashEntry<K, V>[] tab,
			final int i)
	{
		return (tab == null) ? null : (HashEntry<K, V>) DirectMemory
				.getInstance().getObjectVolatile(
						tab,
						((long) i << DirectConcurrentHashMap.TSHIFT)
								+ DirectConcurrentHashMap.TBASE);
	}
	
	/**
	 * Sets the ith element of given table, with volatile write semantics. (See
	 * above about use of putOrderedObject.)
	 */
	static final <K, V> void setEntryAt(final HashEntry<K, V>[] tab,
			final int i, final HashEntry<K, V> e)
	{
		DirectMemory.getInstance().putOrderedObject(
				tab,
				((long) i << DirectConcurrentHashMap.TSHIFT)
						+ DirectConcurrentHashMap.TBASE, e);
	}
	
	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because
	 * DirectConcurrentHashMap uses power-of-two length hash tables, that
	 * otherwise encounter collisions for hashCodes that do not differ in lower
	 * or upper bits.
	 */
	private static int hash(int h)
	{
		// Spread bits to regularize both segment and index locations,
		// using variant of single-word Wang/Jenkins hash.
		h += (h << 15) ^ 0xffffcd7d;
		h ^= (h >>> 10);
		h += (h << 3);
		h ^= (h >>> 6);
		h += (h << 2) + (h << 14);
		return h ^ (h >>> 16);
	}
	
	/**
	 * Segments are specialized versions of hash tables. This subclasses from
	 * ReentrantLock opportunistically, just to simplify some locking and avoid
	 * separate construction.
	 */
	static final class Segment<K, V> extends ReentrantLock implements
			Serializable
	{
		/*
		 * Segments maintain a table of entry lists that are always kept in a
		 * consistent state, so can be read (via volatile reads of segments and
		 * tables) without locking. This requires replicating nodes when
		 * necessary during table resizing, so the old lists can be traversed by
		 * readers still using old version of table.
		 * 
		 * This class defines only mutative methods requiring locking. Except as
		 * noted, the methods of this class perform the per-segment versions of
		 * DirectConcurrentHashMap methods. (Other methods are integrated
		 * directly into DirectConcurrentHashMap methods.) These mutative
		 * methods use a form of controlled spinning on contention via methods
		 * scanAndLock and scanAndLockForPut. These intersperse tryLocks with
		 * traversals to locate nodes. The main benefit is to absorb cache
		 * misses (which are very common for hash tables) while obtaining locks
		 * so that traversal is faster once acquired. We do not actually use the
		 * found nodes since they must be re-acquired under lock anyway to
		 * ensure sequential consistency of updates (and in any case may be
		 * undetectably stale), but they will normally be much faster to
		 * re-locate. Also, scanAndLockForPut speculatively creates a fresh node
		 * to use in put if no node is found.
		 */
		
		private static final long				serialVersionUID	= 2249069246763182397L;
		
		/**
		 * The maximum number of times to tryLock in a prescan before possibly
		 * blocking on acquire in preparation for a locked segment operation. On
		 * multiprocessors, using a bounded number of retries maintains cache
		 * acquired while locating nodes.
		 */
		static final int						MAX_SCAN_RETRIES	= Runtime
																			.getRuntime()
																			.availableProcessors() > 1 ? 64
																			: 1;
		
		/**
		 * The per-segment table. Elements are accessed via entryAt/setEntryAt
		 * providing volatile semantics.
		 */
		transient volatile HashEntry<K, V>[]	table;
		
		/**
		 * The number of elements. Accessed only either within locks or among
		 * other volatile reads that maintain visibility.
		 */
		transient int							count;
		
		/**
		 * The total number of mutative operations in this segment. Even though
		 * this may overflows 32 bits, it provides sufficient accuracy for
		 * stability checks in CHM isEmpty() and size() methods. Accessed only
		 * either within locks or among other volatile reads that maintain
		 * visibility.
		 */
		transient int							modCount;
		
		/**
		 * The table is rehashed when its size exceeds this threshold. (The
		 * value of this field is always <tt>(int)(capacity *
		 * loadFactor)</tt>.)
		 */
		transient int							threshold;
		
		/**
		 * The load factor for the hash table. Even though this value is same
		 * for all segments, it is replicated to avoid needing links to outer
		 * object.
		 * 
		 * @serial
		 */
		final float								loadFactor;
		
		Segment(final float lf, final int threshold, final HashEntry<K, V>[] tab)
		{
			this.loadFactor = lf;
			this.threshold = threshold;
			this.table = tab;
		}
		
		final V put(final K key, final int hash, final V value,
				final boolean onlyIfAbsent)
		{
			HashEntry<K, V> node = this.tryLock() ? null : this
					.scanAndLockForPut(key, hash, value);
			V oldValue;
			try
			{
				final HashEntry<K, V>[] tab = this.table;
				final int index = (tab.length - 1) & hash;
				final HashEntry<K, V> first = DirectConcurrentHashMap.entryAt(
						tab, index);
				for (HashEntry<K, V> e = first;;)
				{
					if (e != null)
					{
						K k;
						if (((k = e.key) == key)
								|| ((e.hash == hash) && key.equals(k)))
						{
							oldValue = e.value;
							if (!onlyIfAbsent)
							{
								e.value = value;
								++this.modCount;
							}
							break;
						}
						e = e.next;
					}
					else
					{
						if (node != null)
						{
							node.setNext(first);
						}
						else
						{
							node = new HashEntry<K, V>(hash, key, value, first);
						}
						final int c = this.count + 1;
						if ((c > this.threshold)
								&& (tab.length < DirectConcurrentHashMap.MAXIMUM_CAPACITY))
						{
							this.rehash(node);
						}
						else
						{
							DirectConcurrentHashMap
									.setEntryAt(tab, index, node);
						}
						++this.modCount;
						this.count = c;
						oldValue = null;
						break;
					}
				}
			}
			finally
			{
				this.unlock();
			}
			return oldValue;
		}
		
		/**
		 * Doubles size of table and repacks entries, also adding the given node
		 * to new table
		 */
		@SuppressWarnings("unchecked")
		private void rehash(final HashEntry<K, V> node)
		{
			/*
			 * Reclassify nodes in each list to new table. Because we are using
			 * power-of-two expansion, the elements from each bin must either
			 * stay at same index, or move with a power of two offset. We
			 * eliminate unnecessary node creation by catching cases where old
			 * nodes can be reused because their next fields won't change.
			 * Statistically, at the default threshold, only about one-sixth of
			 * them need cloning when a table doubles. The nodes they replace
			 * will be garbage collectable as soon as they are no longer
			 * referenced by any reader thread that may be in the midst of
			 * concurrently traversing table. Entry accesses use plain array
			 * indexing because they are followed by volatile table write.
			 */
			final HashEntry<K, V>[] oldTable = this.table;
			final int oldCapacity = oldTable.length;
			final int newCapacity = oldCapacity << 1;
			this.threshold = (int) (newCapacity * this.loadFactor);
			final HashEntry<K, V>[] newTable = new HashEntry[newCapacity];
			final int sizeMask = newCapacity - 1;
			for (int i = 0; i < oldCapacity; i++)
			{
				final HashEntry<K, V> e = oldTable[i];
				if (e != null)
				{
					final HashEntry<K, V> next = e.next;
					final int idx = e.hash & sizeMask;
					if (next == null)
					{
						newTable[idx] = e;
					}
					else
					{ // Reuse consecutive sequence at same slot
						HashEntry<K, V> lastRun = e;
						int lastIdx = idx;
						for (HashEntry<K, V> last = next; last != null; last = last.next)
						{
							final int k = last.hash & sizeMask;
							if (k != lastIdx)
							{
								lastIdx = k;
								lastRun = last;
							}
						}
						newTable[lastIdx] = lastRun;
						// Clone remaining nodes
						for (HashEntry<K, V> p = e; p != lastRun; p = p.next)
						{
							final V v = p.value;
							final int h = p.hash;
							final int k = h & sizeMask;
							final HashEntry<K, V> n = newTable[k];
							newTable[k] = new HashEntry<K, V>(h, p.key, v, n);
						}
					}
				}
			}
			final int nodeIndex = node.hash & sizeMask; // add the new node
			node.setNext(newTable[nodeIndex]);
			newTable[nodeIndex] = node;
			this.table = newTable;
		}
		
		/**
		 * Scans for a node containing given key while trying to acquire lock,
		 * creating and returning one if not found. Upon return, guarantees that
		 * lock is held. UNlike in most methods, calls to method equals are not
		 * screened: Since traversal speed doesn't matter, we might as well help
		 * warm up the associated code and accesses as well.
		 * 
		 * @return a new node if key not found, else null
		 */
		private HashEntry<K, V> scanAndLockForPut(final K key, final int hash,
				final V value)
		{
			HashEntry<K, V> first = DirectConcurrentHashMap.entryForHash(this,
					hash);
			HashEntry<K, V> e = first;
			HashEntry<K, V> node = null;
			int retries = -1; // negative while locating node
			while (!this.tryLock())
			{
				HashEntry<K, V> f; // to recheck first below
				if (retries < 0)
				{
					if (e == null)
					{
						if (node == null)
						{
							node = new HashEntry<K, V>(hash, key, value, null);
						}
						retries = 0;
					}
					else if (key.equals(e.key))
					{
						retries = 0;
					}
					else
					{
						e = e.next;
					}
				}
				else if (++retries > Segment.MAX_SCAN_RETRIES)
				{
					this.lock();
					break;
				}
				else if (((retries & 1) == 0)
						&& ((f = DirectConcurrentHashMap.entryForHash(this,
								hash)) != first))
				{
					e = first = f; // re-traverse if entry changed
					retries = -1;
				}
			}
			return node;
		}
		
		/**
		 * Scans for a node containing the given key while trying to acquire
		 * lock for a remove or replace operation. Upon return, guarantees that
		 * lock is held. Note that we must lock even if the key is not found, to
		 * ensure sequential consistency of updates.
		 */
		private void scanAndLock(final Object key, final int hash)
		{
			// similar to but simpler than scanAndLockForPut
			HashEntry<K, V> first = DirectConcurrentHashMap.entryForHash(this,
					hash);
			HashEntry<K, V> e = first;
			int retries = -1;
			while (!this.tryLock())
			{
				HashEntry<K, V> f;
				if (retries < 0)
				{
					if ((e == null) || key.equals(e.key))
					{
						retries = 0;
					}
					else
					{
						e = e.next;
					}
				}
				else if (++retries > Segment.MAX_SCAN_RETRIES)
				{
					this.lock();
					break;
				}
				else if (((retries & 1) == 0)
						&& ((f = DirectConcurrentHashMap.entryForHash(this,
								hash)) != first))
				{
					e = first = f;
					retries = -1;
				}
			}
		}
		
		/**
		 * Remove; match on key only if value null, else match both.
		 */
		final V remove(final Object key, final int hash, final Object value)
		{
			if (!this.tryLock())
			{
				this.scanAndLock(key, hash);
			}
			V oldValue = null;
			try
			{
				final HashEntry<K, V>[] tab = this.table;
				final int index = (tab.length - 1) & hash;
				HashEntry<K, V> e = DirectConcurrentHashMap.entryAt(tab, index);
				HashEntry<K, V> pred = null;
				while (e != null)
				{
					K k;
					final HashEntry<K, V> next = e.next;
					if (((k = e.key) == key)
							|| ((e.hash == hash) && key.equals(k)))
					{
						final V v = e.value;
						if ((value == null) || (value == v) || value.equals(v))
						{
							if (pred == null)
							{
								DirectConcurrentHashMap.setEntryAt(tab, index,
										next);
							}
							else
							{
								pred.setNext(next);
							}
							++this.modCount;
							--this.count;
							oldValue = v;
						}
						break;
					}
					pred = e;
					e = next;
				}
			}
			finally
			{
				this.unlock();
			}
			return oldValue;
		}
		
		final boolean replace(final K key, final int hash, final V oldValue,
				final V newValue)
		{
			if (!this.tryLock())
			{
				this.scanAndLock(key, hash);
			}
			boolean replaced = false;
			try
			{
				HashEntry<K, V> e;
				for (e = DirectConcurrentHashMap.entryForHash(this, hash); e != null; e = e.next)
				{
					K k;
					if (((k = e.key) == key)
							|| ((e.hash == hash) && key.equals(k)))
					{
						if (oldValue.equals(e.value))
						{
							e.value = newValue;
							++this.modCount;
							replaced = true;
						}
						break;
					}
				}
			}
			finally
			{
				this.unlock();
			}
			return replaced;
		}
		
		final V replace(final K key, final int hash, final V value)
		{
			if (!this.tryLock())
			{
				this.scanAndLock(key, hash);
			}
			V oldValue = null;
			try
			{
				HashEntry<K, V> e;
				for (e = DirectConcurrentHashMap.entryForHash(this, hash); e != null; e = e.next)
				{
					K k;
					if (((k = e.key) == key)
							|| ((e.hash == hash) && key.equals(k)))
					{
						oldValue = e.value;
						e.value = value;
						++this.modCount;
						break;
					}
				}
			}
			finally
			{
				this.unlock();
			}
			return oldValue;
		}
		
		final void clear()
		{
			this.lock();
			try
			{
				final HashEntry<K, V>[] tab = this.table;
				for (int i = 0; i < tab.length; i++)
				{
					DirectConcurrentHashMap.setEntryAt(tab, i, null);
				}
				++this.modCount;
				this.count = 0;
			}
			finally
			{
				this.unlock();
			}
		}
	}
	
	// Accessing segments
	
	/**
	 * Gets the jth element of given segment array (if nonnull) with volatile
	 * element access semantics via Unsafe. (The null check can trigger
	 * harmlessly only during deserialization.) Note: because each element of
	 * segments array is set only once (using fully ordered writes), some
	 * performance-sensitive methods rely on this method only as a recheck upon
	 * null reads.
	 */
	@SuppressWarnings("unchecked")
	static final <K, V> Segment<K, V> segmentAt(final Segment<K, V>[] ss,
			final int j)
	{
		final long u = (j << DirectConcurrentHashMap.SSHIFT)
				+ DirectConcurrentHashMap.SBASE;
		return ss == null ? null : (Segment<K, V>) DirectMemory.getInstance()
				.getObjectVolatile(ss, u);
	}
	
	/**
	 * Returns the segment for the given index, creating it and recording in
	 * segment table (via CAS) if not already present.
	 * 
	 * @param k
	 *            the index
	 * @return the segment
	 */
	@SuppressWarnings("unchecked")
	private Segment<K, V> ensureSegment(final int k)
	{
		final Segment<K, V>[] ss = this.segments;
		final long u = (k << DirectConcurrentHashMap.SSHIFT)
				+ DirectConcurrentHashMap.SBASE; // raw offset
		Segment<K, V> seg;
		if ((seg = (Segment<K, V>) DirectMemory.getInstance()
				.getObjectVolatile(ss, u)) == null)
		{
			final Segment<K, V> proto = ss[0]; // use segment 0 as prototype
			final int cap = proto.table.length;
			final float lf = proto.loadFactor;
			final int threshold = (int) (cap * lf);
			final HashEntry<K, V>[] tab = new HashEntry[cap];
			if ((seg = (Segment<K, V>) DirectMemory.getInstance()
					.getObjectVolatile(ss, u)) == null)
			{ // recheck
				final Segment<K, V> s = new Segment<K, V>(lf, threshold, tab);
				while ((seg = (Segment<K, V>) DirectMemory.getInstance()
						.getObjectVolatile(ss, u)) == null)
				{
					if (DirectMemory.getInstance().compareAndSwapObject(ss, u,
							null, seg = s))
					{
						break;
					}
				}
			}
		}
		return seg;
	}
	
	// Hash-based segment and entry accesses
	
	/**
	 * Get the segment for the given hash
	 */
	@SuppressWarnings("unchecked")
	private Segment<K, V> segmentForHash(final int h)
	{
		final long u = (((h >>> this.segmentShift) & this.segmentMask) << DirectConcurrentHashMap.SSHIFT)
				+ DirectConcurrentHashMap.SBASE;
		return (Segment<K, V>) DirectMemory.getInstance().getObjectVolatile(
				this.segments, u);
	}
	
	/**
	 * Gets the table entry for the given segment and hash
	 */
	@SuppressWarnings("unchecked")
	static final <K, V> HashEntry<K, V> entryForHash(final Segment<K, V> seg,
			final int h)
	{
		HashEntry<K, V>[] tab;
		return ((seg == null) || ((tab = seg.table) == null)) ? null
				: (HashEntry<K, V>) DirectMemory
						.getInstance()
						.getObjectVolatile(
								tab,
								((long) (((tab.length - 1) & h)) << DirectConcurrentHashMap.TSHIFT)
										+ DirectConcurrentHashMap.TBASE);
	}
	
	/* ---------------- Public operations -------------- */
	
	/**
	 * Creates a new, empty map with the specified initial capacity, load factor
	 * and concurrency level.
	 * 
	 * @param initialCapacity
	 *            the initial capacity. The implementation performs internal
	 *            sizing to accommodate this many elements.
	 * @param loadFactor
	 *            the load factor threshold, used to control resizing. Resizing
	 *            may be performed when the average number of elements per bin
	 *            exceeds this threshold.
	 * @param concurrencyLevel
	 *            the estimated number of concurrently updating threads. The
	 *            implementation performs internal sizing to try to accommodate
	 *            this many threads.
	 * @throws IllegalArgumentException
	 *             if the initial capacity is negative or the load factor or
	 *             concurrencyLevel are nonpositive.
	 */
	@SuppressWarnings("unchecked")
	public DirectConcurrentHashMap(int initialCapacity, final float loadFactor,
			int concurrencyLevel)
	{
		if (!(loadFactor > 0) || (initialCapacity < 0)
				|| (concurrencyLevel <= 0))
		{
			throw new IllegalArgumentException();
		}
		if (concurrencyLevel > DirectConcurrentHashMap.MAX_SEGMENTS)
		{
			concurrencyLevel = DirectConcurrentHashMap.MAX_SEGMENTS;
		}
		// Find power-of-two sizes best matching arguments
		int sshift = 0;
		int ssize = 1;
		while (ssize < concurrencyLevel)
		{
			++sshift;
			ssize <<= 1;
		}
		this.segmentShift = 32 - sshift;
		this.segmentMask = ssize - 1;
		if (initialCapacity > DirectConcurrentHashMap.MAXIMUM_CAPACITY)
		{
			initialCapacity = DirectConcurrentHashMap.MAXIMUM_CAPACITY;
		}
		int c = initialCapacity / ssize;
		if ((c * ssize) < initialCapacity)
		{
			++c;
		}
		int cap = DirectConcurrentHashMap.MIN_SEGMENT_TABLE_CAPACITY;
		while (cap < c)
		{
			cap <<= 1;
		}
		// create segments and segments[0]
		final Segment<K, V> s0 = new Segment<K, V>(loadFactor,
				(int) (cap * loadFactor), new HashEntry[cap]);
		final Segment<K, V>[] ss = new Segment[ssize];
		DirectMemory.getInstance().putOrderedObject(ss,
				DirectConcurrentHashMap.SBASE, s0); // ordered
		// write of
		// segments[0]
		this.segments = ss;
	}
	
	/**
	 * Creates a new, empty map with the specified initial capacity and load
	 * factor and with the default concurrencyLevel (16).
	 * 
	 * @param initialCapacity
	 *            The implementation performs internal sizing to accommodate
	 *            this many elements.
	 * @param loadFactor
	 *            the load factor threshold, used to control resizing. Resizing
	 *            may be performed when the average number of elements per bin
	 *            exceeds this threshold.
	 * @throws IllegalArgumentException
	 *             if the initial capacity of elements is negative or the load
	 *             factor is nonpositive
	 * @since 1.6
	 */
	public DirectConcurrentHashMap(final int initialCapacity,
			final float loadFactor)
	{
		this(initialCapacity, loadFactor,
				DirectConcurrentHashMap.DEFAULT_CONCURRENCY_LEVEL);
	}
	
	/**
	 * Creates a new, empty map with the specified initial capacity, and with
	 * default load factor (0.75) and concurrencyLevel (16).
	 * 
	 * @param initialCapacity
	 *            the initial capacity. The implementation performs internal
	 *            sizing to accommodate this many elements.
	 * @throws IllegalArgumentException
	 *             if the initial capacity of elements is negative.
	 */
	public DirectConcurrentHashMap(final int initialCapacity)
	{
		this(initialCapacity, DirectConcurrentHashMap.DEFAULT_LOAD_FACTOR,
				DirectConcurrentHashMap.DEFAULT_CONCURRENCY_LEVEL);
	}
	
	/**
	 * Creates a new, empty map with a default initial capacity (16), load
	 * factor (0.75) and concurrencyLevel (16).
	 */
	public DirectConcurrentHashMap()
	{
		this(DirectConcurrentHashMap.DEFAULT_INITIAL_CAPACITY,
				DirectConcurrentHashMap.DEFAULT_LOAD_FACTOR,
				DirectConcurrentHashMap.DEFAULT_CONCURRENCY_LEVEL);
	}
	
	/**
	 * Creates a new map with the same mappings as the given map. The map is
	 * created with a capacity of 1.5 times the number of mappings in the given
	 * map or 16 (whichever is greater), and a default load factor (0.75) and
	 * concurrencyLevel (16).
	 * 
	 * @param m
	 *            the map
	 */
	public DirectConcurrentHashMap(final Map<? extends K, ? extends V> m)
	{
		this(
				Math.max(
						(int) (m.size() / DirectConcurrentHashMap.DEFAULT_LOAD_FACTOR) + 1,
						DirectConcurrentHashMap.DEFAULT_INITIAL_CAPACITY),
				DirectConcurrentHashMap.DEFAULT_LOAD_FACTOR,
				DirectConcurrentHashMap.DEFAULT_CONCURRENCY_LEVEL);
		this.putAll(m);
	}
	
	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 * 
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	@Override
	public boolean isEmpty()
	{
		/*
		 * Sum per-segment modCounts to avoid mis-reporting when elements are
		 * concurrently added and removed in one segment while checking another,
		 * in which case the table was never actually empty at any point. (The
		 * sum ensures accuracy up through at least 1<<31 per-segment
		 * modifications before recheck.) Methods size() and containsValue() use
		 * similar constructions for stability checks.
		 */
		long sum = 0L;
		final Segment<K, V>[] segments = this.segments;
		for (int j = 0; j < segments.length; ++j)
		{
			final Segment<K, V> seg = DirectConcurrentHashMap.segmentAt(
					segments, j);
			if (seg != null)
			{
				if (seg.count != 0)
				{
					return false;
				}
				sum += seg.modCount;
			}
		}
		if (sum != 0L)
		{ // recheck unless no modifications
			for (int j = 0; j < segments.length; ++j)
			{
				final Segment<K, V> seg = DirectConcurrentHashMap.segmentAt(
						segments, j);
				if (seg != null)
				{
					if (seg.count != 0)
					{
						return false;
					}
					sum -= seg.modCount;
				}
			}
			if (sum != 0L)
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the number of key-value mappings in this map. If the map contains
	 * more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 * 
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size()
	{
		// Try a few times to get accurate count. On failure due to
		// continuous async changes in table, resort to locking.
		final Segment<K, V>[] segments = this.segments;
		int size;
		boolean overflow; // true if size overflows 32 bits
		long sum; // sum of modCounts
		long last = 0L; // previous sum
		int retries = -1; // first iteration isn't retry
		try
		{
			for (;;)
			{
				if (retries++ == DirectConcurrentHashMap.RETRIES_BEFORE_LOCK)
				{
					for (int j = 0; j < segments.length; ++j)
					{
						this.ensureSegment(j).lock(); // force creation
					}
				}
				sum = 0L;
				size = 0;
				overflow = false;
				for (int j = 0; j < segments.length; ++j)
				{
					final Segment<K, V> seg = DirectConcurrentHashMap
							.segmentAt(segments, j);
					if (seg != null)
					{
						sum += seg.modCount;
						final int c = seg.count;
						if ((c < 0) || ((size += c) < 0))
						{
							overflow = true;
						}
					}
				}
				if (sum == last)
				{
					break;
				}
				last = sum;
			}
		}
		finally
		{
			if (retries > DirectConcurrentHashMap.RETRIES_BEFORE_LOCK)
			{
				for (int j = 0; j < segments.length; ++j)
				{
					DirectConcurrentHashMap.segmentAt(segments, j).unlock();
				}
			}
		}
		return overflow ? Integer.MAX_VALUE : size;
	}
	
	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if this map contains no mapping for the key.
	 * <p>
	 * More formally, if this map contains a mapping from a key {@code k} to a
	 * value {@code v} such that {@code key.equals(k)}, then this method returns
	 * {@code v}; otherwise it returns {@code null}. (There can be at most one
	 * such mapping.)
	 * 
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V get(final Object key)
	{
		Segment<K, V> s; // manually integrate access methods to reduce overhead
		HashEntry<K, V>[] tab;
		final int h = DirectConcurrentHashMap.hash(key.hashCode());
		final long u = (((h >>> this.segmentShift) & this.segmentMask) << DirectConcurrentHashMap.SSHIFT)
				+ DirectConcurrentHashMap.SBASE;
		if (((s = (Segment<K, V>) DirectMemory.getInstance().getObjectVolatile(
				this.segments, u)) != null)
				&& ((tab = s.table) != null))
		{
			for (HashEntry<K, V> e = (HashEntry<K, V>) DirectMemory
					.getInstance()
					.getObjectVolatile(
							tab,
							((long) (((tab.length - 1) & h)) << DirectConcurrentHashMap.TSHIFT)
									+ DirectConcurrentHashMap.TBASE); e != null; e = e.next)
			{
				K k;
				if (((k = e.key) == key) || ((e.hash == h) && key.equals(k)))
				{
					return e.value;
				}
			}
		}
		return null;
	}
	
	/**
	 * Tests if the specified object is a key in this table.
	 * 
	 * @param key
	 *            possible key
	 * @return <tt>true</tt> if and only if the specified object is a key in
	 *         this table, as determined by the <tt>equals</tt> method;
	 *         <tt>false</tt> otherwise.
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean containsKey(final Object key)
	{
		Segment<K, V> s; // same as get() except no need for volatile value read
		HashEntry<K, V>[] tab;
		final int h = DirectConcurrentHashMap.hash(key.hashCode());
		final long u = (((h >>> this.segmentShift) & this.segmentMask) << DirectConcurrentHashMap.SSHIFT)
				+ DirectConcurrentHashMap.SBASE;
		if (((s = (Segment<K, V>) DirectMemory.getInstance().getObjectVolatile(
				this.segments, u)) != null)
				&& ((tab = s.table) != null))
		{
			for (HashEntry<K, V> e = (HashEntry<K, V>) DirectMemory
					.getInstance()
					.getObjectVolatile(
							tab,
							((long) (((tab.length - 1) & h)) << DirectConcurrentHashMap.TSHIFT)
									+ DirectConcurrentHashMap.TBASE); e != null; e = e.next)
			{
				K k;
				if (((k = e.key) == key) || ((e.hash == h) && key.equals(k)))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the specified
	 * value. Note: This method requires a full internal traversal of the hash
	 * table, and so is much slower than method <tt>containsKey</tt>.
	 * 
	 * @param value
	 *            value whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map maps one or more keys to the specified
	 *         value
	 * @throws NullPointerException
	 *             if the specified value is null
	 */
	@Override
	public boolean containsValue(final Object value)
	{
		// Same idea as size()
		if (value == null)
		{
			throw new NullPointerException();
		}
		final Segment<K, V>[] segments = this.segments;
		boolean found = false;
		long last = 0;
		int retries = -1;
		try
		{
			outer: for (;;)
			{
				if (retries++ == DirectConcurrentHashMap.RETRIES_BEFORE_LOCK)
				{
					for (int j = 0; j < segments.length; ++j)
					{
						this.ensureSegment(j).lock(); // force creation
					}
				}
				int sum = 0;
				for (int j = 0; j < segments.length; ++j)
				{
					HashEntry<K, V>[] tab;
					final Segment<K, V> seg = DirectConcurrentHashMap
							.segmentAt(segments, j);
					if ((seg != null) && ((tab = seg.table) != null))
					{
						for (int i = 0; i < tab.length; i++)
						{
							HashEntry<K, V> e;
							for (e = DirectConcurrentHashMap.entryAt(tab, i); e != null; e = e.next)
							{
								final V v = e.value;
								if ((v != null) && value.equals(v))
								{
									found = true;
									break outer;
								}
							}
						}
						sum += seg.modCount;
					}
				}
				if ((retries > 0) && (sum == last))
				{
					break;
				}
				last = sum;
			}
		}
		finally
		{
			if (retries > DirectConcurrentHashMap.RETRIES_BEFORE_LOCK)
			{
				for (int j = 0; j < segments.length; ++j)
				{
					DirectConcurrentHashMap.segmentAt(segments, j).unlock();
				}
			}
		}
		return found;
	}
	
	/**
	 * Legacy method testing if some key maps into the specified value in this
	 * table. This method is identical in functionality to
	 * {@link #containsValue}, and exists solely to ensure full compatibility
	 * with class {@link java.util.Hashtable}, which supported this method prior
	 * to introduction of the Java Collections framework.
	 * 
	 * @param value
	 *            a value to search for
	 * @return <tt>true</tt> if and only if some key maps to the <tt>value</tt>
	 *         argument in this table as determined by the <tt>equals</tt>
	 *         method; <tt>false</tt> otherwise
	 * @throws NullPointerException
	 *             if the specified value is null
	 */
	public boolean contains(final Object value)
	{
		return this.containsValue(value);
	}
	
	/**
	 * Maps the specified key to the specified value in this table. Neither the
	 * key nor the value can be null.
	 * <p>
	 * The value can be retrieved by calling the <tt>get</tt> method with a key
	 * that is equal to the original key.
	 * 
	 * @param key
	 *            key with which the specified value is to be associated
	 * @param value
	 *            value to be associated with the specified key
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
	 *         if there was no mapping for <tt>key</tt>
	 * @throws NullPointerException
	 *             if the specified key or value is null
	 */
	@Override
	@SuppressWarnings("unchecked")
	public V put(final K key, final V value)
	{
		Segment<K, V> s;
		if (value == null)
		{
			throw new NullPointerException();
		}
		final int hash = DirectConcurrentHashMap.hash(key.hashCode());
		final int j = (hash >>> this.segmentShift) & this.segmentMask;
		if ((s = (Segment<K, V>) DirectMemory.getInstance().getObject // nonvolatile;
																		// recheck
				(this.segments,
						(j << DirectConcurrentHashMap.SSHIFT)
								+ DirectConcurrentHashMap.SBASE)) == null)
		{
			s = this.ensureSegment(j);
		}
		return s.put(key, hash, value, false);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @return the previous value associated with the specified key, or
	 *         <tt>null</tt> if there was no mapping for the key
	 * @throws NullPointerException
	 *             if the specified key or value is null
	 */
	@Override
	@SuppressWarnings("unchecked")
	public V putIfAbsent(final K key, final V value)
	{
		Segment<K, V> s;
		if (value == null)
		{
			throw new NullPointerException();
		}
		final int hash = DirectConcurrentHashMap.hash(key.hashCode());
		final int j = (hash >>> this.segmentShift) & this.segmentMask;
		if ((s = (Segment<K, V>) DirectMemory.getInstance().getObject(
				this.segments,
				(j << DirectConcurrentHashMap.SSHIFT)
						+ DirectConcurrentHashMap.SBASE)) == null)
		{
			s = this.ensureSegment(j);
		}
		return s.put(key, hash, value, true);
	}
	
	/**
	 * Copies all of the mappings from the specified map to this one. These
	 * mappings replace any mappings that this map had for any of the keys
	 * currently in the specified map.
	 * 
	 * @param m
	 *            mappings to be stored in this map
	 */
	@Override
	public void putAll(final Map<? extends K, ? extends V> m)
	{
		for (final Map.Entry<? extends K, ? extends V> e : m.entrySet())
		{
			this.put(e.getKey(), e.getValue());
		}
	}
	
	/**
	 * Removes the key (and its corresponding value) from this map. This method
	 * does nothing if the key is not in the map.
	 * 
	 * @param key
	 *            the key that needs to be removed
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
	 *         if there was no mapping for <tt>key</tt>
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	@Override
	public V remove(final Object key)
	{
		final int hash = DirectConcurrentHashMap.hash(key.hashCode());
		final Segment<K, V> s = this.segmentForHash(hash);
		return s == null ? null : s.remove(key, hash, null);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	@Override
	public boolean remove(final Object key, final Object value)
	{
		final int hash = DirectConcurrentHashMap.hash(key.hashCode());
		Segment<K, V> s;
		return (value != null) && ((s = this.segmentForHash(hash)) != null)
				&& (s.remove(key, hash, value) != null);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws NullPointerException
	 *             if any of the arguments are null
	 */
	@Override
	public boolean replace(final K key, final V oldValue, final V newValue)
	{
		final int hash = DirectConcurrentHashMap.hash(key.hashCode());
		if ((oldValue == null) || (newValue == null))
		{
			throw new NullPointerException();
		}
		final Segment<K, V> s = this.segmentForHash(hash);
		return (s != null) && s.replace(key, hash, oldValue, newValue);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @return the previous value associated with the specified key, or
	 *         <tt>null</tt> if there was no mapping for the key
	 * @throws NullPointerException
	 *             if the specified key or value is null
	 */
	@Override
	public V replace(final K key, final V value)
	{
		final int hash = DirectConcurrentHashMap.hash(key.hashCode());
		if (value == null)
		{
			throw new NullPointerException();
		}
		final Segment<K, V> s = this.segmentForHash(hash);
		return s == null ? null : s.replace(key, hash, value);
	}
	
	/**
	 * Removes all of the mappings from this map.
	 */
	@Override
	public void clear()
	{
		final Segment<K, V>[] segments = this.segments;
		for (int j = 0; j < segments.length; ++j)
		{
			final Segment<K, V> s = DirectConcurrentHashMap.segmentAt(segments,
					j);
			if (s != null)
			{
				s.clear();
			}
		}
	}
	
	/**
	 * Returns a {@link Set} view of the keys contained in this map. The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa. The set supports element removal, which removes the
	 * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations. It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 * <p>
	 * The view's <tt>iterator</tt> is a "weakly consistent" iterator that will
	 * never throw {@link ConcurrentModificationException}, and guarantees to
	 * traverse elements as they existed upon construction of the iterator, and
	 * may (but is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 */
	@Override
	public Set<K> keySet()
	{
		final Set<K> ks = this.keySet;
		return (ks != null) ? ks : (this.keySet = new KeySet());
	}
	
	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * The collection is backed by the map, so changes to the map are reflected
	 * in the collection, and vice-versa. The collection supports element
	 * removal, which removes the corresponding mapping from this map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt>, and <tt>clear</tt> operations. It does not support
	 * the <tt>add</tt> or <tt>addAll</tt> operations.
	 * <p>
	 * The view's <tt>iterator</tt> is a "weakly consistent" iterator that will
	 * never throw {@link ConcurrentModificationException}, and guarantees to
	 * traverse elements as they existed upon construction of the iterator, and
	 * may (but is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 */
	@Override
	public Collection<V> values()
	{
		final Collection<V> vs = this.values;
		return (vs != null) ? vs : (this.values = new Values());
	}
	
	/**
	 * Returns a {@link Set} view of the mappings contained in this map. The set
	 * is backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa. The set supports element removal, which removes the
	 * corresponding mapping from the map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations. It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 * <p>
	 * The view's <tt>iterator</tt> is a "weakly consistent" iterator that will
	 * never throw {@link ConcurrentModificationException}, and guarantees to
	 * traverse elements as they existed upon construction of the iterator, and
	 * may (but is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet()
	{
		final Set<Map.Entry<K, V>> es = this.entrySet;
		return (es != null) ? es : (this.entrySet = new EntrySet());
	}
	
	/**
	 * Returns an enumeration of the keys in this table.
	 * 
	 * @return an enumeration of the keys in this table
	 * @see #keySet()
	 */
	public Enumeration<K> keys()
	{
		return new KeyIterator();
	}
	
	/**
	 * Returns an enumeration of the values in this table.
	 * 
	 * @return an enumeration of the values in this table
	 * @see #values()
	 */
	public Enumeration<V> elements()
	{
		return new ValueIterator();
	}
	
	/* ---------------- Iterator Support -------------- */
	
	abstract class HashIterator
	{
		int					nextSegmentIndex;
		int					nextTableIndex;
		HashEntry<K, V>[]	currentTable;
		HashEntry<K, V>		nextEntry;
		HashEntry<K, V>		lastReturned;
		
		HashIterator()
		{
			this.nextSegmentIndex = DirectConcurrentHashMap.this.segments.length - 1;
			this.nextTableIndex = -1;
			this.advance();
		}
		
		/**
		 * Set nextEntry to first node of next non-empty table (in backwards
		 * order, to simplify checks).
		 */
		final void advance()
		{
			for (;;)
			{
				if (this.nextTableIndex >= 0)
				{
					if ((this.nextEntry = DirectConcurrentHashMap.entryAt(
							this.currentTable, this.nextTableIndex--)) != null)
					{
						break;
					}
				}
				else if (this.nextSegmentIndex >= 0)
				{
					final Segment<K, V> seg = DirectConcurrentHashMap
							.segmentAt(DirectConcurrentHashMap.this.segments,
									this.nextSegmentIndex--);
					if ((seg != null)
							&& ((this.currentTable = seg.table) != null))
					{
						this.nextTableIndex = this.currentTable.length - 1;
					}
				}
				else
				{
					break;
				}
			}
		}
		
		final HashEntry<K, V> nextEntry()
		{
			final HashEntry<K, V> e = this.nextEntry;
			if (e == null)
			{
				throw new NoSuchElementException();
			}
			this.lastReturned = e; // cannot assign until after null check
			if ((this.nextEntry = e.next) == null)
			{
				this.advance();
			}
			return e;
		}
		
		public final boolean hasNext()
		{
			return this.nextEntry != null;
		}
		
		public final boolean hasMoreElements()
		{
			return this.nextEntry != null;
		}
		
		public final void remove()
		{
			if (this.lastReturned == null)
			{
				throw new IllegalStateException();
			}
			DirectConcurrentHashMap.this.remove(this.lastReturned.key);
			this.lastReturned = null;
		}
	}
	
	final class KeyIterator extends HashIterator implements Iterator<K>,
			Enumeration<K>
	{
		@Override
		public final K next()
		{
			return super.nextEntry().key;
		}
		
		@Override
		public final K nextElement()
		{
			return super.nextEntry().key;
		}
	}
	
	final class ValueIterator extends HashIterator implements Iterator<V>,
			Enumeration<V>
	{
		@Override
		public final V next()
		{
			return super.nextEntry().value;
		}
		
		@Override
		public final V nextElement()
		{
			return super.nextEntry().value;
		}
	}
	
	/**
	 * Custom Entry class used by EntryIterator.next(), that relays setValue
	 * changes to the underlying map.
	 */
	final class WriteThroughEntry extends AbstractMap.SimpleEntry<K, V>
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= -6279845213765974859L;
		
		WriteThroughEntry(final K k, final V v)
		{
			super(k, v);
		}
		
		/**
		 * Set our entry's value and write through to the map. The value to
		 * return is somewhat arbitrary here. Since a WriteThroughEntry does not
		 * necessarily track asynchronous changes, the most recent "previous"
		 * value could be different from what we return (or could even have been
		 * removed in which case the put will re-establish). We do not and
		 * cannot guarantee more.
		 */
		@Override
		public V setValue(final V value)
		{
			if (value == null)
			{
				throw new NullPointerException();
			}
			final V v = super.setValue(value);
			DirectConcurrentHashMap.this.put(this.getKey(), value);
			return v;
		}
	}
	
	final class EntryIterator extends HashIterator implements
			Iterator<Entry<K, V>>
	{
		@Override
		public Map.Entry<K, V> next()
		{
			final HashEntry<K, V> e = super.nextEntry();
			return new WriteThroughEntry(e.key, e.value);
		}
	}
	
	final class KeySet extends AbstractSet<K>
	{
		@Override
		public Iterator<K> iterator()
		{
			return new KeyIterator();
		}
		
		@Override
		public int size()
		{
			return DirectConcurrentHashMap.this.size();
		}
		
		@Override
		public boolean isEmpty()
		{
			return DirectConcurrentHashMap.this.isEmpty();
		}
		
		@Override
		public boolean contains(final Object o)
		{
			return DirectConcurrentHashMap.this.containsKey(o);
		}
		
		@Override
		public boolean remove(final Object o)
		{
			return DirectConcurrentHashMap.this.remove(o) != null;
		}
		
		@Override
		public void clear()
		{
			DirectConcurrentHashMap.this.clear();
		}
	}
	
	final class Values extends AbstractCollection<V>
	{
		@Override
		public Iterator<V> iterator()
		{
			return new ValueIterator();
		}
		
		@Override
		public int size()
		{
			return DirectConcurrentHashMap.this.size();
		}
		
		@Override
		public boolean isEmpty()
		{
			return DirectConcurrentHashMap.this.isEmpty();
		}
		
		@Override
		public boolean contains(final Object o)
		{
			return DirectConcurrentHashMap.this.containsValue(o);
		}
		
		@Override
		public void clear()
		{
			DirectConcurrentHashMap.this.clear();
		}
	}
	
	final class EntrySet extends AbstractSet<Map.Entry<K, V>>
	{
		@Override
		public Iterator<Map.Entry<K, V>> iterator()
		{
			return new EntryIterator();
		}
		
		@Override
		public boolean contains(final Object o)
		{
			if (!(o instanceof Map.Entry))
			{
				return false;
			}
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			final V v = DirectConcurrentHashMap.this.get(e.getKey());
			return (v != null) && v.equals(e.getValue());
		}
		
		@Override
		public boolean remove(final Object o)
		{
			if (!(o instanceof Map.Entry))
			{
				return false;
			}
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			return DirectConcurrentHashMap.this
					.remove(e.getKey(), e.getValue());
		}
		
		@Override
		public int size()
		{
			return DirectConcurrentHashMap.this.size();
		}
		
		@Override
		public boolean isEmpty()
		{
			return DirectConcurrentHashMap.this.isEmpty();
		}
		
		@Override
		public void clear()
		{
			DirectConcurrentHashMap.this.clear();
		}
	}
	
	/* ---------------- Serialization Support -------------- */
	
	/**
	 * Save the state of the <tt>DirectConcurrentHashMap</tt> instance to a
	 * stream (i.e., serialize it).
	 * 
	 * @param s
	 *            the stream
	 * @serialData the key (Object) and value (Object) for each key-value
	 *             mapping, followed by a null pair. The key-value mappings are
	 *             emitted in no particular order.
	 */
	private void writeObject(final java.io.ObjectOutputStream s)
			throws IOException
	{
		// force all segments for serialization compatibility
		for (int k = 0; k < this.segments.length; ++k)
		{
			this.ensureSegment(k);
		}
		s.defaultWriteObject();
		
		final Segment<K, V>[] segments = this.segments;
		for (int k = 0; k < segments.length; ++k)
		{
			final Segment<K, V> seg = DirectConcurrentHashMap.segmentAt(
					segments, k);
			seg.lock();
			try
			{
				final HashEntry<K, V>[] tab = seg.table;
				for (int i = 0; i < tab.length; ++i)
				{
					HashEntry<K, V> e;
					for (e = DirectConcurrentHashMap.entryAt(tab, i); e != null; e = e.next)
					{
						s.writeObject(e.key);
						s.writeObject(e.value);
					}
				}
			}
			finally
			{
				seg.unlock();
			}
		}
		s.writeObject(null);
		s.writeObject(null);
	}
	
	/**
	 * Reconstitute the <tt>DirectConcurrentHashMap</tt> instance from a stream
	 * (i.e., deserialize it).
	 * 
	 * @param s
	 *            the stream
	 */
	@SuppressWarnings("unchecked")
	private void readObject(final java.io.ObjectInputStream s)
			throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		
		// Re-initialize segments to be minimally sized, and let grow.
		final int cap = DirectConcurrentHashMap.MIN_SEGMENT_TABLE_CAPACITY;
		final Segment<K, V>[] segments = this.segments;
		for (int k = 0; k < segments.length; ++k)
		{
			final Segment<K, V> seg = segments[k];
			if (seg != null)
			{
				seg.threshold = (int) (cap * seg.loadFactor);
				seg.table = new HashEntry[cap];
			}
		}
		
		// Read the keys and values, and put the mappings in the table
		for (;;)
		{
			final K key = (K) s.readObject();
			final V value = (V) s.readObject();
			if (key == null)
			{
				break;
			}
			this.put(key, value);
		}
	}
	
	// Unsafe mechanics
	private static final long	SBASE;
	private static final int	SSHIFT;
	private static final long	TBASE;
	private static final int	TSHIFT;
	
	static
	{
		int ss, ts;
		try
		{
			final Class tc = HashEntry[].class;
			final Class sc = Segment[].class;
			TBASE = DirectMemory.getInstance().arrayBaseOffset(tc);
			SBASE = DirectMemory.getInstance().arrayBaseOffset(sc);
			ts = DirectMemory.getInstance().arrayIndexScale(tc);
			ss = DirectMemory.getInstance().arrayIndexScale(sc);
		}
		catch (final Exception e)
		{
			throw new Error(e);
		}
		if (((ss & (ss - 1)) != 0) || ((ts & (ts - 1)) != 0))
		{
			throw new Error("data type scale not a power of two");
		}
		SSHIFT = 31 - Integer.numberOfLeadingZeros(ss);
		TSHIFT = 31 - Integer.numberOfLeadingZeros(ts);
	}
	
}
