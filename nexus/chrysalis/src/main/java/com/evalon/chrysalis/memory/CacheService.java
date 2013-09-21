package com.evalon.chrysalis.memory;

import com.evalon.chrysalis.memory.pointers.Pointer;
import com.evalon.chrysalis.util.serialization.Serializer;

import java.util.concurrent.ConcurrentMap;

public interface CacheService<K, V>
{
	
	void scheduleDisposalEvery(long l);
	
	/**
	 * @param key
	 * @param payload
	 * @param expiresIn
	 *            in ms
	 * @return
	 */
	Pointer<V> putByteArray(K key, byte[] payload, int expiresIn);
	
	Pointer<V> putByteArray(K key, byte[] payload);
	
	Pointer<V> put(K key, V value);
	
	/**
	 * @param key
	 * @param value
	 * @param expiresIn
	 *            in ms
	 * @return
	 */
	Pointer<V> put(K key, V value, int expiresIn);
	
	byte[] retrieveByteArray(K key);
	
	V retrieve(K key);
	
	Pointer<V> getPointer(K key);
	
	void free(K key);
	
	void free(Pointer<V> pointer);
	
	void collectExpired();
	
	void collectAll();
	
	void clear();
	
	long entries();
	
	void dump();
	
	ConcurrentMap<K, Pointer<V>> getMap();
	
	void setMap(ConcurrentMap<K, Pointer<V>> map);
	
	Serializer getSerializer();
	
	MemoryManager<V> getMemoryManager();
	
	void setMemoryManager(MemoryManager<V> memoryManager);
	
	void setSerializer(Serializer serializer);
	
	<T extends V> Pointer<V> allocate(K key, Class<T> type, int size);
	
}
