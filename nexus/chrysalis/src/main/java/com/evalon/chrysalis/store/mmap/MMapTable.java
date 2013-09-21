/**
 * 
 */
package com.evalon.chrysalis.store.mmap;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.memory.direct.Direct;
import com.evalon.chrysalis.store.Column;
import com.evalon.chrysalis.store.Row;
import com.evalon.chrysalis.store.Table;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class MMapTable implements Table
{
	private final Map<String, Integer>				rowKeys	= new LinkedHashMap<String, Integer>();
	// private final List<MMapRow> rows = new LinkedList<MMapRow>();
	private final LinkedHashMap<Integer, MMapRow>	rows	= new LinkedHashMap<Integer, MMapRow>();
	private final Map<String, MMapColumn<?>>		columns	= new LinkedHashMap<String, MMapColumn<?>>();
	private final Map<String, Object>				metaMap	= new HashMap<String, Object>();
	private final FileChannel						tableFileChannel;
	private final ByteBuffer						sizeBuffer;
	private final ByteBuffer						metaBuffer;
	private final MMapColumn<Long>					timeRevisions;
	private final String							name;
	private int										size;
	private final String							targetDir;
	
	public MMapTable(String baseDir, String name) throws IOException
	{
		this.targetDir = baseDir;
		this.name = name;
		new File(baseDir).mkdirs();
		this.tableFileChannel = new RandomAccessFile(new File(baseDir,
				this.name + ".metastore.meta"), "rw").getChannel();
		this.sizeBuffer = this.tableFileChannel.map(
				FileChannel.MapMode.READ_WRITE, 0, Direct.SIZE_OF_FLOAT)
		// .load()
				.order(ByteOrder.nativeOrder());
		this.metaBuffer = this.tableFileChannel.map(
				FileChannel.MapMode.READ_WRITE, Direct.SIZE_OF_FLOAT,
				(Integer.MAX_VALUE / 8))// .load()
				.order(ByteOrder.nativeOrder());
		this.timeRevisions = new MMapColumnLong(this, "time");
		this.size = sizeBuffer.getInt(0);
	}
	
	public static final class DirectMeta<T> implements Meta<T>
	{
		final MMapTable	table;
		final String	key;
		T				value;
		
		public DirectMeta(MMapTable dt, String k, T v)
		{
			this.table = dt;
			this.key = k;
			this.value = v;
		}
		
		public String getKey()
		{
			return this.key;
		}
		
		public Table setValue(T meta) throws Exception
		{
			this.value = meta;
			this.table.setMeta(this);
			return this.table;
		}
		
		@SuppressWarnings("unchecked")
		public T getValue()
		{
			return (T) ((Meta<T>) this.table.metaMap.get(key)).getValue();
		}
		
		public Table getTable()
		{
			return this.table;
		}
	}
	
	public long size()
	{
		return size;
	}
	
	public Row newRow()
	{
		return this.newRow(String.valueOf(this.size));
	}
	
	public Row newRow(String key)
	{
		MMapRow tmpRow = null;
		synchronized (this.rows)
		{
			int index = (int) this.size;
			tmpRow = new MMapRow(this).setIndex(index).setKey(key);
			this.rows.put(index, tmpRow);
			this.rowKeys.put(key, index);
			this.incrSize();
		}
		return tmpRow;
	}
	
	public <T extends Column<?>> Column<?> newColumn(String name, Class<T> cls)
			throws InstantiationException, Exception, AssertionError
	{
		return ((Column<?>) DirectMemory.getInstance().allocateInstance(cls))
				.loadInitiation(this, name);
	}
	
	public void drop() throws Exception
	{
		this.close();
	}
	
	public void close() throws Exception
	{
		for (MMapColumn<?> directColumn : columns.values())
		{
			directColumn.close();
		}
	}
	
	public long getLastRevisionOfRow(int index)
	{
		return this.timeRevisions.get(index);
	}
	
	private long incrSize()
	{
		sizeBuffer.putInt(0, (size + 1));
		return ++size;
	}
	
	public String getBaseDirectory()
	{
		return this.targetDir;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getMeta(String key)
	{
		return (T) this.metaMap.get(key);
	}
	
	public <T> Meta<T> newMeta(String key, T meta)
	{
		this.metaMap.put(key, meta);
		return null;
	}
	
	public <T> void setMeta(Meta<T> meta) throws Exception
	{
		this.metaMap.put(meta.getKey(), meta);
		this.setMeta(meta.getKey(), meta.getValue());
	}
	
	@SuppressWarnings("unchecked")
	public <T> void setMeta(String key, T metaV) throws Exception
	{
		((Meta<T>) this.metaMap.get(key)).setValue(metaV);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] tmpByteArr = null;
		try
		{
			out = new ObjectOutputStream(bos);
			out.writeObject(this.metaMap);
			tmpByteArr = bos.toByteArray();
		}
		finally
		{
			bos.close();
			out.close();
		}
		metaBuffer.put(tmpByteArr, 0, tmpByteArr.length);
	}
	
	public String name()
	{
		return this.name;
	}
	
	public List<String> listColumnNames()
	{
		return new ArrayList<String>(columns.keySet());
	}
	
	public List<Column<?>> listColumns()
	{
		return new ArrayList<Column<?>>(columns.values());
	}
	
	public Row row(String key)
	{
		return this.row(this.rowKeys.get(key));
	}
	
	public Row row(int index)
	{
		return this.rows.get(index);
	}
	
	public <T> Row set(int i, Column<T> column, T value) throws IOException
	{
		return this.rows.get(i).set(column, value);
	}
	
	public <T> Row set(String key, Column<T> column, T value)
			throws IOException
	{
		return this.set(this.rowKeys.get(key), column, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Column<T> column(String key)
	{
		return (Column<T>) this.columns.get(key);
	}
	
	public Object column(String key, int index)
	{
		return this.columns.get(key).get(index);
	}
	
	public <T> Row lock(String key, int index) throws IOException
	{
		this.columns.get(key).lock(index);
		return this.row(index);
	}
	
	public <T> Row lock(int index) throws IOException
	{
		for (Column<?> col : this.listColumns())
		{
			try
			{
				col.lock(index);
			}
			catch (Exception e)
			{
				
			}
		}
		return this.row(index);
	}
	
	public <T> Row release(String key, int index) throws IOException
	{
		this.columns.get(key).release(index);
		return this.row(index);
	}
	
	public <T> Row release(int index) throws IOException
	{
		for (Column<?> col : this.listColumns())
		{
			try
			{
				col.release(index);
			}
			catch (Exception e)
			{
				
			}
		}
		return this.row(index);
	}
}
