package com.evalon.chrysalis.store;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Column<T>
{
	Column<T> loadInitiation(Table table, String name)
			throws FileNotFoundException, IOException;
	
	String getName();
	
	T get(int index);
	
	Column<T> set(int index, T value) throws IOException;
	
	Column<T> setSynced(int index, T value);
	
	Column<T> lock(int index) throws IOException;
	
	Column<T> release(int index) throws IOException;
	
	int getChunkDataSize();
	
	void close() throws IOException;
}
