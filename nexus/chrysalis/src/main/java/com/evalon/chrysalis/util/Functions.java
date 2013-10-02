package com.evalon.chrysalis.util;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Set of common functions used by the Disruptor
 */
@SuppressWarnings("restriction")
public final class Functions
{
	/**
	 * Calculate the log base 2 of the supplied integer, essentially reports the
	 * location of the highest bit.
	 * 
	 * @param i
	 *            Value to calculate log2 for.
	 * @return The log2 value
	 */
	public static int log2(int i)
	{
		int r = 0;
		while ((i >>= 1) != 0)
		{
			++r;
		}
		return r;
	}
	
	/**
	 * Gets the address value for the memory that backs a direct byte buffer.
	 * 
	 * @param buffer
	 * @return The system address for the buffers
	 */
	public static long getAddressFromDirectByteBuffer(ByteBuffer buffer)
	{
		try
		{
			Field addrField = Buffer.class.getDeclaredField("address");
			addrField.setAccessible(true);
			return addrField.getLong(buffer);
		}
		catch (Exception e)
		{
			throw new RuntimeException(
					"Unable to address field from ByteBuffer", e);
		}
	}
	
	/**
	 * allocate a new DirectBuffer with the given Capacity
	 * 
	 * @param size
	 *            Allocation Size
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public static DirectBuffer newDirectBuffer(int size)
	{
		return (DirectBuffer) ByteBuffer.allocateDirect(size);
	}
	
	/**
	 * Calculate the next power of 2, greater than or equal to x.
	 * <p>
	 * From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
	 * 
	 * @param x
	 *            Value to round up
	 * @return The next power of 2 from x inclusive
	 */
	public static int nextPowerOfTwo(final int x)
	{
		return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
	}
	
	/**
	 * Byte Serialization
	 * 
	 * @param obj
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public static byte[] serialize(final Object obj)
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try
		{
			oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	/**
	 * Deserialize from Byte array
	 * 
	 * @param arr
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public static Object deSerialize(final byte[] arr)
	{
		ObjectInputStream ois = null;
		Object obj = null;
		try
		{
			ois = new ObjectInputStream(new ByteArrayInputStream(arr));
			obj = ois.readObject();
			ois.close();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		catch (final ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		return obj;
	}
	
	/**
	 * Erase the x-th element and shrink to new size
	 * 
	 * @param x
	 * @param tArr
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] eraseAndShrink(int x, T[] tArr)
	{
		int r = x - 1;
		final T tmpArr[] = tArr.clone();
		tArr = null;
		tArr = (T[]) Array.newInstance(tmpArr[0].getClass(), tmpArr.length - 1);
		// Arrays.copyOf(tmpArr, tmpArr.length - 1);
		System.arraycopy(tmpArr, 0, tArr, 0, r);
		System.arraycopy(tmpArr, x, tArr, r, tArr.length - r);
		return tArr;
	}
	
	/**
	 * @param index
	 * @param tArr
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public static <T> T[] erase(int index, T[] tArr)
	{
		System.arraycopy(tArr, 0, tArr, 0, (index <= 0 ? 1 : index) - 1);
		System.arraycopy(tArr, index + 1, tArr, index, tArr.length - index - 1);
		tArr[tArr.length - 1] = null;
		return tArr;
	}
	
	/**
	 * Throws NullPointerException if argument is null.
	 * 
	 * @param v
	 *            the element
	 */
	public final static void checkNotNull(Object v)
	{
		if (v == null) throw new NullPointerException();
	}
	
	private static final Unsafe	THE_UNSAFE;
	static
	{
		try
		{
			final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			THE_UNSAFE = (Unsafe) theUnsafe.get(null);
		}
		catch (final Exception e)
		{
			throw new RuntimeException("Unable to load unsafe", e);
		}
	}
	
	public static Unsafe getUnsafe()
	{
		return Functions.THE_UNSAFE;
	}
}
