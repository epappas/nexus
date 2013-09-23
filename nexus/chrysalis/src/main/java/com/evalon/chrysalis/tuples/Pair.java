/**
 * 
 */
package com.evalon.chrysalis.tuples;

/**
 * A pattern to return double individual values
 * 
 * @author Evangelos Pappas - Evalon.gr
 */
public class Pair<L, R>
{
	private L		left;
	private R		right;
	private boolean	hasReturnLeft;
	
	/**
	 * @param l
	 * @param r
	 */
	public Pair(final L l, final R r)
	{
		this.left = l;
		this.right = r;
		this.hasReturnLeft = false;
	}
	
	/**
	 * 
	 */
	public Pair()
	{
		this(null, null);
	}
	
	/**
	 * Returns both but individually. first left, after right and reverse
	 * 
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public Object getAny()
	{
		this.hasReturnLeft = !this.hasReturnLeft;
		if (!this.hasReturnLeft)
		{
			return this.left;
		}
		return this.right;
	}
	
	/**
	 * return the left value
	 * 
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public L getLeft()
	{
		return this.left;
	}
	
	/**
	 * return the right value
	 * 
	 * @return
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public R getRight()
	{
		return this.right;
	}
	
	/**
	 * @param left
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public void setLeft(final L left)
	{
		this.left = left;
	}
	
	/**
	 * @param right
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public void setRight(final R right)
	{
		this.right = right;
	}
}
