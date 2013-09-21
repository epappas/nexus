package com.evalon.chrysalis.memory;

import com.evalon.chrysalis.memory.pointers.DirectPointer;
import com.evalon.chrysalis.memory.pointers.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DirectMemoryManager<V> extends AbstractMemoryManager<V>
{
	protected static Logger			logger		= LoggerFactory
														.getLogger(DirectMemoryManager.class);
	private final Set<Pointer<V>>	pointers	= Collections
														.newSetFromMap(new ConcurrentHashMap<Pointer<V>, Boolean>());
	
	public void init(final int size)
	{
		this.capacity = size;
	}
	
	protected Pointer<V> createPointer(final long expires,
			final TimeUnit timeUnit)
	{
		final Pointer<V> p = new DirectPointer<V>();
		p.setExpiration(expires, timeUnit);
		
		this.pointers.add(p);
		
		return p;
	}
	
	@Override
	public Pointer<V> store(final byte[] payload, final long expires,
			TimeUnit timeUnit)
	{
		if ((this.capacity - this.used.get() - payload.length) < 0)
		{
			throw new BufferOverflowException();
		}
		
		final Pointer<V> p = this.createPointer(expires, timeUnit);
		p.store(payload);
		
		this.used.addAndGet(payload.length);
		return p;
	}
	
	@Override
	public Pointer<V> update(final Pointer<V> pointer, final byte[] payload)
	{
		pointer.store(payload);
		return pointer;
	}
	
	public byte[] retrieve(final Pointer<V> pointer)
	{
		return pointer.value();
	}
	
	@Override
	public Pointer<V> free(final Pointer<V> pointer)
	{
		this.pointers.remove(pointer);
		this.used.set(this.used.get() - (pointer.getSize()));
		pointer.free();
		return pointer;
	}
}
