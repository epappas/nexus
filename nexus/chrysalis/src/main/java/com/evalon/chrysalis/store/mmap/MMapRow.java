/**
 * 
 */
package com.evalon.chrysalis.store.mmap;

import com.evalon.chrysalis.store.Column;
import com.evalon.chrysalis.store.Row;

import java.io.IOException;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class MMapRow implements Row
{
	private final MMapTable	mMapTable;
	private int				index	= 0;
	private String			key		= "0";
	
	public MMapRow(MMapTable mMapTable)
	{
		this.mMapTable = mMapTable;
	}
	
	public MMapRow next()
	{
		return null;
	}
	
	public MMapRow setIndex(int index)
	{
		this.index = index;
		return this;
	}
	
	public int getIndex()
	{
		return this.index;
	}
	
	public boolean hasNext()
	{
		return index++ < mMapTable.size();
	}
	
	public long when()
	{
		return mMapTable.getLastRevisionOfRow(index);
	}
	
	public <T> T get(Column<T> column)
	{
		return ((MMapColumn<T>) column).get(index);
	}
	
	public <T> MMapRow set(Column<T> column, T value) throws IOException
	{
		((MMapColumn<T>) column).set(index, value);
		return this;
	}
	
	public MMapRow setKey(String key)
	{
		this.key = key;
		return this;
	}
	
	public String getKey()
	{
		return this.key;
	}
	
	public <T> MMapRow setSynced(Column<T> column, T value)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public <T> MMapRow lock() throws IOException
	{
		return null;
	}
	
	public <T> MMapRow lock(Column<T> column) throws IOException
	{
		return null;
	}
	
	public <T> MMapRow release() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public <T> MMapRow release(Column<T> column) throws IOException
	{
		
		return this;
	}
}
