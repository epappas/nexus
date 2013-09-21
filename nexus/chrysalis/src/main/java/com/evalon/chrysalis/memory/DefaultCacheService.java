package com.evalon.chrysalis.memory;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import com.evalon.chrysalis.memory.pointers.Pointer;
import com.evalon.chrysalis.util.metrics.Sizing;
import com.evalon.chrysalis.util.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class DefaultCacheService<K, V> implements CacheService<K, V>
{
	
	private static Logger					logger	= LoggerFactory
															.getLogger(DefaultCacheService.class);
	
	private ConcurrentMap<K, Pointer<V>>	map;
	
	private Serializer						serializer;
	
	private MemoryManager<V>				memoryManager;
	
	private final Timer						timer	= new Timer(true);
	
	/**
	 * Constructor
	 */
	public DefaultCacheService(final ConcurrentMap<K, Pointer<V>> map,
			final MemoryManager<V> memoryManager, final Serializer serializer)
	{
		this.map = map;
		this.memoryManager = memoryManager;
		this.serializer = serializer;
	}
	
	public void scheduleDisposalEvery(final long l)
	{
		this.timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				DefaultCacheService.logger.info("begin scheduled disposal");
				
				DefaultCacheService.this.collectExpired();
				
				DefaultCacheService.logger.info("scheduled disposal complete");
			}
		}, l, l);
		
		DefaultCacheService.logger.info(
				"disposal scheduled every {} milliseconds", l);
	}
	
	public Pointer<V> putByteArray(final K key, final byte[] payload)
	{
		return this.store(key, payload, 0);
	}
	
	public Pointer<V> putByteArray(final K key, final byte[] payload,
			final int expiresIn)
	{
		return this.store(key, payload, expiresIn);
	}
	
	public Pointer<V> put(final K key, final V value)
	{
		return this.put(key, value, 0);
	}
	
	public Pointer<V> put(final K key, final V value, final int expiresIn)
	{
		try
		{
			final byte[] payload = this.serializer.serialize(value);
			final Pointer<V> ptr = this.store(key, payload, expiresIn);
			if (ptr != null)
			{
				@SuppressWarnings("unchecked")
				// type driven by the compiler
				final Class<? extends V> clazz = (Class<? extends V>) value
						.getClass();
				
				ptr.setClazz(clazz);
			}
			return ptr;
		}
		catch (final IOException e)
		{
			
			if (DefaultCacheService.logger.isDebugEnabled())
			{
				DefaultCacheService.logger
						.debug("IOException put object in cache:{}",
								e.getMessage(), e);
			}
			else
			{
				DefaultCacheService.logger.error(
						"IOException put object in cache:{}", e.getMessage());
			}
			return null;
		}
	}
	
	private Pointer<V> store(final K key, final byte[] payload,
			final int expiresIn)
	{
		Pointer<V> pointer = this.map.get(key);
		if (pointer != null)
		{
			this.memoryManager.free(pointer);
		}
		pointer = this.memoryManager.store(payload, expiresIn);
		if (pointer != null)
		{
			this.map.put(key, pointer);
		}
		return pointer;
	}
	
	public byte[] retrieveByteArray(final K key)
	{
		final Pointer<V> ptr = this.getPointer(key);
		if (ptr == null)
		{
			return null;
		}
		if (ptr.isExpired() || ptr.isFree())
		{
			this.map.remove(key);
			if (!ptr.isFree())
			{
				this.memoryManager.free(ptr);
			}
			return null;
		}
		else
		{
			return this.memoryManager.retrieve(ptr);
		}
	}
	
	public V retrieve(final K key)
	{
		final Pointer<V> ptr = this.getPointer(key);
		if (ptr == null)
		{
			return null;
		}
		if (ptr.isExpired() || ptr.isFree())
		{
			this.map.remove(key);
			if (!ptr.isFree())
			{
				this.memoryManager.free(ptr);
			}
			return null;
		}
		else
		{
			try
			{
				return this.serializer.deserialize(
						this.memoryManager.retrieve(ptr), ptr.getClazz());
			}
			catch (final Exception e)
			{
				DefaultCacheService.logger.error(e.getMessage());
			}
		}
		return null;
	}
	
	public Pointer<V> getPointer(final K key)
	{
		return this.map.get(key);
	}
	
	public void free(final K key)
	{
		final Pointer<V> p = this.map.remove(key);
		if (p != null)
		{
			this.memoryManager.free(p);
		}
	}
	
	public void free(final Pointer<V> pointer)
	{
		this.memoryManager.free(pointer);
	}
	
	public void collectExpired()
	{
		this.memoryManager.collectExpired();
		// still have to look for orphan (storing references to freed pointers)
		// map entries
	}
	
	public void collectAll()
	{
		final Thread thread = new Thread()
		{
			@Override
			public void run()
			{
				DefaultCacheService.logger.info("begin disposal");
				DefaultCacheService.this.collectExpired();
				DefaultCacheService.logger.info("disposal complete");
			}
		};
		thread.start();
	}
	
	public void clear()
	{
		this.map.clear();
		this.memoryManager.clear();
		DefaultCacheService.logger.info("Cache cleared");
	}
	
	public long entries()
	{
		return this.map.size();
	}
	
	public void dump(final MemoryManager<V> mms)
	{
		DefaultCacheService.logger.info(String.format(
				"off-heap - allocated: \t%1s", Sizing.inMb(mms.capacity())));
		DefaultCacheService.logger.info(String.format(
				"off-heap - used:      \t%1s", Sizing.inMb(mms.used())));
		DefaultCacheService.logger.info(String.format("heap  - max: \t%1s",
				Sizing.inMb(Runtime.getRuntime().maxMemory())));
		DefaultCacheService.logger.info(String.format(
				"heap     - allocated: \t%1s",
				Sizing.inMb(Runtime.getRuntime().totalMemory())));
		DefaultCacheService.logger.info(String.format(
				"heap     - free : \t%1s",
				Sizing.inMb(Runtime.getRuntime().freeMemory())));
		DefaultCacheService.logger
				.info("************************************************");
	}
	
	public void dump()
	{
		if (!DefaultCacheService.logger.isInfoEnabled())
		{
			return;
		}
		
		DefaultCacheService.logger
				.info("*** CacheBuilder statistics ********************");
		
		this.dump(this.memoryManager);
	}
	
	public ConcurrentMap<K, Pointer<V>> getMap()
	{
		return this.map;
	}
	
	public void setMap(final ConcurrentMap<K, Pointer<V>> map)
	{
		this.map = map;
	}
	
	public Serializer getSerializer()
	{
		return this.serializer;
	}
	
	public void setSerializer(final Serializer serializer)
	{
		this.serializer = serializer;
	}
	
	public MemoryManager<V> getMemoryManager()
	{
		return this.memoryManager;
	}
	
	public void setMemoryManager(final MemoryManager<V> memoryManager)
	{
		this.memoryManager = memoryManager;
	}
	
	public <T extends V> Pointer<V> allocate(final K key, final Class<T> type,
			final int size)
	{
		final Pointer<V> ptr = this.memoryManager.allocate(type, size, 0,
				TimeUnit.NANOSECONDS);
		this.map.put(key, ptr);
		ptr.setClazz(type);
		return ptr;
	}
}
