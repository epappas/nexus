package com.evalon.chrysalis.memory;

import com.evalon.chrysalis.memory.pointers.Pointer;
import com.evalon.chrysalis.util.metrics.In;
import com.evalon.chrysalis.util.serialization.Serializer;
import com.evalon.chrysalis.util.serialization.SerializerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CacheBuilder<K, V>
{
	
	public static final int					DEFAULT_CONCURRENCY_LEVEL	= 4;
	public static final int					DEFAULT_INITIAL_CAPACITY	= 100000;
	public static final int					DEFAULT_DISPOSAL_TIME		= 10;												// seconds
																															
	private int								size;
	private int								initialCapacity				= CacheBuilder.DEFAULT_INITIAL_CAPACITY;
	private int								concurrencyLevel			= CacheBuilder.DEFAULT_CONCURRENCY_LEVEL;
	private long							disposalTime				= In.seconds(CacheBuilder.DEFAULT_DISPOSAL_TIME);
	private MemoryManager<V>				memoryManager				= new DirectMemoryManager<V>();
	private Serializer						serializer					= SerializerFactory
																				.createNewSerializer();
	private ConcurrentMap<K, Pointer<V>>	map							= new ConcurrentHashMap<K, Pointer<V>>(
																				this.initialCapacity,
																				1024,
																				this.concurrencyLevel);
	
	public CacheBuilder()
	{
		
	}
	
	public CacheBuilder(final CacheBuilder<K, V> prototype)
	{
		this.size = prototype.size;
		this.initialCapacity = prototype.initialCapacity;
		this.concurrencyLevel = prototype.concurrencyLevel;
		this.disposalTime = prototype.disposalTime;
		
		this.map = prototype.map;
		this.serializer = prototype.serializer;
		this.memoryManager = prototype.memoryManager;
	}
	
	public CacheBuilder<K, V> setSize(final int size)
	{
		this.size = size;
		return this;
	}
	
	public CacheBuilder<K, V> setInitialCapacity(final int initialCapacity)
	{
		this.initialCapacity = initialCapacity;
		return this;
	}
	
	public CacheBuilder<K, V> setConcurrencyLevel(final int concurrencyLevel)
	{
		this.concurrencyLevel = concurrencyLevel;
		return this;
	}
	
	public CacheBuilder<K, V> setDisposalTime(final long disposalTime)
	{
		this.disposalTime = disposalTime;
		return this;
	}
	
	public CacheBuilder<K, V> setMap(final ConcurrentMap<K, Pointer<V>> map)
	{
		this.map = map;
		return this;
	}
	
	public CacheBuilder<K, V> setSerializer(final Serializer serializer)
	{
		this.serializer = serializer;
		return this;
	}
	
	public CacheBuilder<K, V> setMemoryManager(
			final MemoryManager<V> memoryManager)
	{
		this.memoryManager = memoryManager;
		return this;
	}
	
	public CacheService<K, V> build()
	{
		this.memoryManager.init(this.size);
		
		final CacheService<K, V> cacheService = new DefaultCacheService<K, V>(
				this.map, this.memoryManager, this.serializer);
		cacheService.scheduleDisposalEvery(this.disposalTime);
		return cacheService;
	}
}
