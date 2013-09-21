/**
 * 
 */
package com.evalon.chrysalis.memory.direct;

import java.io.*;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class DirectObject<K extends Object> implements Direct<K>
{
	protected DirectBytes	directBytes;
	
	public DirectObject(K obj) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeObject(obj);
		
		this.directBytes = new DirectBytes(bos.toByteArray());
		bos.close();
		out.close();
	}
	
	public long set(K obj)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try
		{
			out = new ObjectOutputStream(bos);
			out.writeObject(obj);
			this.directBytes.set(bos.toByteArray());
		}
		catch (Exception e)
		{
			
		}
		finally
		{
			try
			{
				bos.close();
				out.close();
			}
			catch (Exception e)
			{
				
			}
		}
		
		return this.directBytes.getSize();
	}
	
	@SuppressWarnings("unchecked")
	public K get()
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(
				this.directBytes.get());
		ObjectInput in = null;
		Object tmp = null;
		try
		{
			in = new ObjectInputStream(bis);
			tmp = in.readObject();
			return (K) tmp;
		}
		catch (Exception e)
		{
			
		}
		finally
		{
			try
			{
				bis.close();
				in.close();
			}
			catch (Exception e)
			{
				
			}
		}
		return (K) tmp;
	}
	
	public void dispose()
	{
		this.directBytes.dispose();
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.dispose();
		super.finalize();
	}
	
	public long getAddress()
	{
		return this.directBytes.getAddress();
	}
	
	public void setAddress(long addr)
	{
		this.directBytes.setAddress(addr);
	}
	
	public int getSize()
	{
		return this.directBytes.getSize();
	}
	
}
