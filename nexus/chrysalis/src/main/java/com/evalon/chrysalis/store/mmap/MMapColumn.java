/**
 * 
 */
package com.evalon.chrysalis.store.mmap;

import com.evalon.chrysalis.store.Column;
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
import java.util.Map;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
@SuppressWarnings("restriction")
public abstract class MMapColumn<T> implements Column<T>
{
	private String						name;
	private FileChannel					fc;
	protected Map<Integer, FileLock>	lockMap;
	protected ByteBuffer				bb;
	protected MMapTable					table;
	protected long						address;
	private int							size;
	
	MMapColumn(Table table, String name) throws IOException
	{
		this.loadInitiation(table, name);
	}
	
	public String getName()
	{
		return name;
	}
	
	public abstract T get(int index);
	
	public abstract MMapColumn<T> set(int index, T value) throws IOException;
	
	public abstract MMapColumn<T> setSynced(int index, T value);
	
	public abstract MMapColumn<T> lock(int index) throws IOException;
	
	public abstract MMapColumn<T> release(int index) throws IOException;
	
	public abstract int getChunkDataSize();
	
	public Column<T> loadInitiation(Table table, String name)
			throws IOException
	{
		this.table = (MMapTable) table;
		File file = new File(this.table.getBaseDirectory() + "/"
				+ this.table.name() + "." + name + ".data");
		this.name = file.getName();
		this.fc = new RandomAccessFile(file, "rw").getChannel();
		this.size = Integer.MAX_VALUE / (8 * this.getChunkDataSize());
		this.bb = this.fc.map(FileChannel.MapMode.READ_WRITE, 0, size)// .load()
				.order(ByteOrder.nativeOrder());
		this.address = ((DirectBuffer) bb).address();
		this.lockMap = new HashMap<Integer, FileLock>();
		return this;
	}
	
	public FileChannel getFileChannel()
	{
		return this.fc;
	}
	
	public void close() throws IOException
	{
		fc.close();
	}
}
