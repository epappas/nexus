package com.evalon.chrysalis.memory;

import com.evalon.chrysalis.memory.pointers.Pointer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface MemoryManager<V>
{
	
	void init(int size);
	
	Pointer<V> store(byte[] payload, long expires, TimeUnit timeUnit);
	
	Pointer<V> store(byte[] payload, long expiresIn);
	
	Pointer<V> store(byte[] payload);
	
	Pointer<V> update(Pointer<V> pointer, byte[] payload);
	
	byte[] retrieve(Pointer<V> pointer);
	
	Pointer<V> free(Pointer<V> pointer);
	
	void clear();
	
	long capacity();
	
	long used();
	
	long collectExpired();
	
	<T extends V> Pointer<V> allocate(Class<T> type, int size,
			final long expires, TimeUnit timeUnit);
	
	Set<Pointer<V>> getPointers();
	
}
