/**
 * 
 */
package com.evalon.chrysalis.store.mmap;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.memory.direct.Direct;
import com.evalon.chrysalis.store.Table;

import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class MMapColumnLong extends MMapColumn<Long>
{
	public MMapColumnLong(Table table, String name) throws IOException
	{
		super(table, name);
	}
	
	public Long get(int index)
	{
		return DirectMemory.getInstance().getLong(
				address + (index * Direct.SIZE_OF_LONG));
	}
	
	public MMapColumnLong set(int index, Long value)
	{
		DirectMemory.getInstance().putLong(
				address + (index * Direct.SIZE_OF_LONG), value);
		return this;
	}
	
	public int getChunkDataSize()
	{
		return Direct.SIZE_OF_LONG;
	}
	
	@Override
	public MMapColumn<Long> setSynced(int index, Long value)
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
	public MMapColumn<Long> lock(int index) throws IOException
	{
		this.lockMap.put(
				index,
				this.getFileChannel().lock(
						this.address + (index * Direct.SIZE_OF_LONG),
						Direct.SIZE_OF_LONG, true));
		return this;
	}
	
	@Override
	public MMapColumn<Long> release(int index) throws IOException
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
