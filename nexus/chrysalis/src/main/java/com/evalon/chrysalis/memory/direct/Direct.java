/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import com.evalon.chrysalis.memory.DirectMemory;
import com.evalon.chrysalis.util.Disposable;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Direct<T> extends Disposable
{
	static final long	BOOLEAN_ARRAY_OFFSET	= DirectMemory
														.getInstance()
														.arrayBaseOffset(
																boolean[].class);
	static final long	BYTE_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																byte[].class);
	static final long	SHORT_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																short[].class);
	static final long	CHAR_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																char[].class);
	static final long	INT_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																int[].class);
	static final long	FLOAT_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																float[].class);
	static final long	LONG_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																long[].class);
	static final long	DOUBLE_ARRAY_OFFSET		= DirectMemory.getInstance()
														.arrayBaseOffset(
																double[].class);
	// 8 reserved bits, 1 used
	static final int	SIZE_OF_BOOLEAN			= 1;
	// 8 reserved bits,
	static final int	SIZE_OF_BYTES			= 1;
	// 16 reserved bits,
	static final int	SIZE_OF_SHORT			= 2;
	// 16 reserved bits,
	static final int	SIZE_OF_CHAR			= 2;
	// 32 reserved bits,
	static final int	SIZE_OF_INT				= 4;
	// 32 reserved bits,
	static final int	SIZE_OF_FLOAT			= 4;
	// 64 reserved bits,
	static final int	SIZE_OF_LONG			= 8;
	// 64 reserved bits,
	static final int	SIZE_OF_DOUBLE			= 8;
	
	long set(T obj);
		
	T get();
	
	int getSize();
	
	long getAddress();
	
	void setAddress(long addr);
	
	public static class PaddedLong
	{
		public long	value	= 0;
		@SuppressWarnings("unused")
		private long	p1, p2, p3, p4, p5, p6;
	}
}
