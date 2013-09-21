
package com.evalon.chrysalis.store.mmap;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.memory.direct.Direct;
import com.evalon.chrysalis.store.Table;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
@SuppressWarnings("restriction")
public class CopyOfMMapColumnBytes extends MMapColumn<byte[]>
{
	private HashMap<Integer, Integer>		objectSizes			= new HashMap<Integer, Integer>();
	private HashMap<Integer, ByteBuffer>	objectByteBuffers	= new HashMap<Integer, ByteBuffer>();
	private HashMap<Integer, FileChannel>	objectFChannels		= new HashMap<Integer, FileChannel>();
	private long							pos					= 0L;
	
	public CopyOfMMapColumnBytes(Table table, String name) throws IOException
	{
		super(table, name);
	}
	
	public byte[] get(int index)
	{
		if (index >= this.pos)
		{
			return null;
		}
		// ByteBuffer bb = this.objectByteBuffers.get(index);
		
		int length = this.objectSizes.get(index);
		long addr = DirectMemory.getInstance().getLong(
				address + (index * Direct.SIZE_OF_LONG));
		byte[] valBArr = new byte[length];
		for (int i = 0; i < length; i++)
		{
			valBArr[i] = DirectMemory.getInstance().getByte(addr + (i));
		}
		
		return valBArr;
	}
	
	public CopyOfMMapColumnBytes set(int index, byte[] value) throws IOException
	{
		int length = value.length;
		int size = length * Direct.SIZE_OF_BYTES;
		long addr = 0;
		if (index >= this.pos)
		{
			File file = new File(this.table.getBaseDirectory() + "/"
					+ this.table.name() + "." + getName() + "." + index
					+ ".data");
			FileChannel fc = new RandomAccessFile(file, "rw").getChannel();
			ByteBuffer bb = fc.map(FileChannel.MapMode.READ_WRITE, 0, size)
					.order(ByteOrder.nativeOrder());
			
			addr = ((DirectBuffer) bb).address();
			
			DirectMemory.getInstance().putLong(
					address + (index * Direct.SIZE_OF_LONG), addr);
			
			this.objectFChannels.put(index, fc);
			this.objectByteBuffers.put(index, bb);
			this.objectSizes.put(index, size);
			this.pos = index + 1;
		}
		else
		{
			addr = DirectMemory.getInstance().getLong(
					address + (index * Direct.SIZE_OF_LONG));
		}
		
		for (int i = 0; i < length; i++)
		{
			DirectMemory.getInstance().putByte(addr + i, value[i]);
		}
		
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
