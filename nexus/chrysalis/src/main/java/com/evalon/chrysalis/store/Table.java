/**
 * 
 */
package com.evalon.chrysalis.store;

import java.io.IOException;
import java.util.List;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Table
{
	long size();
	
	String name();
	
	Row newRow();
	
	Row newRow(String key);
	
	List<String> listColumnNames();
	
	List<Column<?>> listColumns();
	
	Row row(String key);
	
	Row row(int index);
	
	<T> Column<T> column(String key);
	
	Object column(String key, int index);
	
	<T> Row set(int i, Column<T> column, T value) throws IOException;
	
	<T> Row set(String key, Column<T> column, T value) throws IOException;
	
	<T> Row lock(String key, int index) throws IOException;
	
	<T> Row lock(int index) throws IOException;
	
	<T> Row release(String key, int index) throws IOException;
	
	<T> Row release(int index) throws IOException;
	
	<T extends Column<?>> Column<?> newColumn(String name, Class<T> cls)
			throws InstantiationException, Exception, AssertionError;
	
	<T> Meta<T> newMeta(String key, T metaV);
	
	<T> T getMeta(String key);
	
	<T> void setMeta(String key, T metaV) throws Exception;
	
	<T> void setMeta(Meta<T> meta) throws Exception;
	
	void drop() throws Exception;
	
	void close() throws Exception;
	
	long getLastRevisionOfRow(int index);
	
	String getBaseDirectory();
	
	static interface Meta<T>
	{
		String getKey();
		
		T getValue();
		
		Table setValue(T metaV) throws Exception;
		
		Table getTable();
	}
}
