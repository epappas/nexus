package com.evalon.chrysalis.store.mmap;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.memory.direct.Direct;
import com.evalon.chrysalis.store.Column;
import com.evalon.chrysalis.store.Table;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
@SuppressWarnings("restriction")
public class MMapColumnBytes extends MMapColumn<byte[]>
{
	private ArrayList<Integer>	objectSizes	= new ArrayList<Integer>();
	private ArrayList<Long>		addresses	= new ArrayList<Long>();
	private long				pos			= 0L;
	
	public MMapColumnBytes(Table table, String name) throws IOException
	{
		super(table, name);
		this.pos = this.address;
	}
	
	@Override
	public Column<byte[]> loadInitiation(Table table, String name)
			throws IOException
	{
		super.loadInitiation(table, name);
		objectSizes = new ArrayList<Integer>();
		addresses = new ArrayList<Long>();
		this.pos = this.address;
		return this;
	}
	
	public byte[] get(int index)
	{
		int length = 0;
		long addr = 0L;
		try
		{
			addr = addresses.get(index);
			length = this.objectSizes.get(index);
		}
		catch (IndexOutOfBoundsException e)
		{
			return null;
		}
		byte[] valBArr = new byte[length];
		for (int i = 0; i < length; i++)
		{
			valBArr[i] = DirectMemory.getInstance().getByte(addr + (i));
		}
		return valBArr;
	}
	
	public MMapColumnBytes set(int index, byte[] value) throws IOException
	{
		int length = value.length;
		long addr = 0;
		try
		{
			addr = addresses.get(index);
		}
		catch (IndexOutOfBoundsException e)
		{
			addr = this.pos;
			addresses.add(addr);
		}
		
		for (int i = 0; i < length; i++)
		{
			DirectMemory.getInstance().putByte(addr + i, value[i]);
		}
		objectSizes.add(length);
		this.pos = addr + length;
		return this;
	}
	
	public int getChunkDataSize()
	{
		// Size of each addr pointer
		return Direct.SIZE_OF_LONG;
	}
	
	@Override
	public MMapColumn<byte[]> setSynced(int index, byte[] value)
	{
		try
		{
			FileLock flPTR = this.getFileChannel().tryLock(
					this.address + (index * Direct.SIZE_OF_LONG),
					Direct.SIZE_OF_LONG, true);
			this.set(index, value);
			flPTR.release();
		}
		catch (IOException e)
		{
			
		}
		return this;
	}
	
	@Override
	public MMapColumn<byte[]> lock(int index) throws IOException
	{
		this.lockMap.put(
				index,
				this.getFileChannel().lock(
						this.address + (index * Direct.SIZE_OF_LONG),
						Direct.SIZE_OF_LONG, true));
		return this;
	}
	
	@Override
	public MMapColumn<byte[]> release(int index) throws IOException
	{
		try
		{
			FileLock flPTR = this.lockMap.remove(index);
			if (flPTR != null)
			{
				flPTR.release();
			}
		}
		catch (Exception e)
		{
			throw new IOException();
		}
		return this;
	}
}
