package com.evalon.chrysalis.memory;

import com.evalon.chrysalis.memory.pointers.Pointer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractMemoryManager<V> implements
		MemoryManager<V>
{
	protected static final long		NEVER_EXPIRES	= 0L;
	protected final Set<Pointer<V>>	pointers		= Collections
															.newSetFromMap(new ConcurrentHashMap<Pointer<V>, Boolean>());
	protected final AtomicLong		used			= new AtomicLong(0L);
	protected long					capacity;
	
	public AbstractMemoryManager()
	{
		super();
	}
	
	abstract public void init(int size);
	
	abstract public Pointer<V> free(Pointer<V> pointer);
	
	abstract public Pointer<V> store(byte[] payload, long expires,
			TimeUnit timeUnit);
	
	public Pointer<V> store(byte[] payload, long expiresIn)
	{
		return this.store(payload, 0, TimeUnit.NANOSECONDS);
	}
	
	public Pointer<V> store(final byte[] payload)
	{
		return this.store(payload, 0, TimeUnit.NANOSECONDS);
	}
	
	public Pointer<V> update(final Pointer<V> pointer, final byte[] payload)
	{
		pointer.store(payload);
		return pointer;
	}
	
	public long used()
	{
		return this.used.get();
	}
	
	public byte[] retrieve(final Pointer<V> pointer)
	{
		return pointer.value();
	}
	
	public long collectExpired()
	{
		// TODO final int limit = 50;
		List<Pointer<V>> pL = new ArrayList<Pointer<V>>();
		for (Pointer<V> p : pointers)
		{
			if (p.isExpired())
			{
				pL.add(p);
			}
		}
		return this.free(pL);
		
	}
	
	protected long free(final Iterable<Pointer<V>> pointers)
	{
		long count = 0;
		for (final Pointer<V> expired : pointers)
		{
			count += expired.getSize();
			this.free(expired);
		}
		return count;
	}
	
	public Set<Pointer<V>> getPointers()
	{
		return Collections.unmodifiableSet(this.pointers);
	}
	
	public <T extends V> Pointer<V> allocate(final Class<T> type,
			final int size, final long expires, TimeUnit timeUnit)
	{
		final Pointer<V> p = this.store(new byte[size], expires, timeUnit);
		
		if (p != null)
		{
			p.setClazz(type);
		}
		
		return p;
	}
	
	public void clear()
	{
		for (final Pointer<V> pointer : this.pointers)
		{
			this.free(pointer);
		}
	}
	
	public long capacity()
	{
		return this.capacity;
	}
	
}
