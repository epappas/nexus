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
public class MMapColumnInt extends MMapColumn<Integer>
{
	public MMapColumnInt(Table table, String name) throws IOException
	{
		super(table, name);
	}
	
	public Integer get(int index)
	{
		return DirectMemory.getInstance().getInt(
				address + (index * Direct.SIZE_OF_INT));
	}
	
	public MMapColumnInt set(int index, Integer value)
	{
		DirectMemory.getInstance().putInt(
				address + (index * Direct.SIZE_OF_INT), value);
		return this;
	}
	
	public int getChunkDataSize()
	{
		return Direct.SIZE_OF_INT;
	}
	
	@Override
	public MMapColumn<Integer> setSynced(int index, Integer value)
	{
		try
		{
			FileLock flPTR = this.getFileChannel().tryLock(this.address,
					Direct.SIZE_OF_INT, true);
			this.set(index, value);
			flPTR.release();
		}
		catch (IOException e)
		{
			
		}
		return this;
	}
	
	@Override
	public MMapColumn<Integer> lock(int index) throws IOException
	{
		this.lockMap.put(
				index,
				this.getFileChannel().lock(
						this.address + (index * Direct.SIZE_OF_INT),
						Direct.SIZE_OF_INT, true));
		return this;
	}
	
	@Override
	public MMapColumn<Integer> release(int index) throws IOException
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
