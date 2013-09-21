/**
 * 
 */
package com.evalon.chrysalis.store;

import java.io.IOException;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Row
{
	Row next();
	
	boolean hasNext();
	
	long when();
	
	<T> T get(Column<T> column);
	
	<T> Row set(Column<T> column, T value) throws IOException;
	
	<T> Row setSynced(Column<T> column, T value);
	
	<T> Row lock() throws IOException;
	
	<T> Row lock(Column<T> column) throws IOException;
	
	<T> Row release() throws IOException;
	
	<T> Row release(Column<T> column) throws IOException;
	
	Row setIndex(int index);
	
	Row setKey(String key);
	
	int getIndex();
	
	String getKey();
}
