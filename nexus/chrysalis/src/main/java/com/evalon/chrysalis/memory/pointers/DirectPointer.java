package com.evalon.chrysalis.memory.pointers;

import com.evalon.chrysalis.memory.direct.DirectBytes;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <T>
 */
@SuppressWarnings("restriction")
public class DirectPointer<T> implements Pointer<T>
{
	private long				expires;
	private boolean				free;
	private Class<? extends T>	clazz;
	private DirectBytes			directStore;
	private DirectBuffer		directBuffer	= null;
	private String				key;
	private int					keyHash;
	
	public DirectPointer()
	{
		this.expires = 0;
		this.free = true;
		this.clazz = null;
		this.directStore = null;
	}
	
	public DirectPointer(final long addr, final int size)
	{
		this();
		this.directStore = new DirectBytes(addr, size);
	}
	
	public DirectPointer(String key, final long addr, final int size)
	{
		this(addr, size);
		this.setKey(key);
	}
	
	@Override
	public String toString()
	{
		return String
				.format("{ \"class\":\"%s\", \"addr\":\"%s\", \"size\":\"%s\", \"expires\":\"%s\", \"free\":%s }",
						this.getClass().getSimpleName(), this.directStore
								.getAddress(), this.directStore.getSize(),
						this.expires, (this.free ? "true" : "false"));
	}
	
	public void reset()
	{
		this.free = true;
		this.clazz = null;
		this.directBuffer = null;
		this.expires = 0;
		if (this.directStore != null)
		{
			this.directStore.dispose();
		}
	}
	
	public boolean isFree()
	{
		return this.free;
	}
	
	public boolean isExpired()
	{
		return (this.expires < System.nanoTime());
	}
	
	public long getSize()
	{
		return this.directStore.getSize();
	}
	
	public Class<? extends T> getClazz()
	{
		return this.clazz;
	}
	
	public ByteBuffer getDirectBuffer()
	{
		return (ByteBuffer) this.directBuffer;
	}
	
	public void setFree(final boolean free)
	{
		this.free = free;
	}
	
	public void setClazz(final Class<? extends T> clazz)
	{
		this.clazz = clazz;
	}
	
	public void setDirectBuffer(final ByteBuffer directBuffer)
	{
		this.directBuffer = (DirectBuffer) directBuffer;
		this.directStore.dispose();
		this.directStore = new DirectBytes(this.directBuffer.address(),
				directBuffer.capacity());
	}
	
	public void setExpiration(final long expires, TimeUnit timeUnit)
	{
		this.expires = TimeUnit.NANOSECONDS.convert(expires, timeUnit);
	}
	
	public long getExpires()
	{
		return this.expires;
	}
	
	public long getExpires(TimeUnit timeUnit)
	{
		return timeUnit.convert(this.expires, TimeUnit.NANOSECONDS);
	}
	
	public Pointer<T> newInstance()
	{
		return new DirectPointer<T>();
	}
	
	public long store(byte[] payload)
	{
		if (this.directStore == null)
		{
			this.directStore = new DirectBytes(payload);
		}
		else
		{
			this.directStore.set(payload);
		}
		return this.directStore.getAddress();
	}
	
	public byte[] value()
	{
		return this.directStore.get();
	}
	
	public void free()
	{
		this.reset();
		this.directStore.dispose();
	}
	
	public long getAddress()
	{
		return this.directStore.getAddress();
	}
	
	public DirectPointer<T> setAddress(long address)
	{
		if (this.directStore == null)
		{
			this.directStore = new DirectBytes(address, 0);
		}
		else
		{
			this.directStore.setAddress(address);
		}
		return this;
	}
	
	@Override
	public void setKey(String key)
	{
		this.key = key;
		this.keyHash = this.key.hashCode();
	}
	
	@Override
	public String getKey()
	{
		return this.key;
	}
	
	@Override
	public <T> T getKeyHash()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
