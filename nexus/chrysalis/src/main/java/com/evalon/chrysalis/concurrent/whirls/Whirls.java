package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.memory.pointers.Pointer;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public enum Whirls
{
	Threaded
	{
		@Override
		public <V extends Pointer<?>> Whirl<V> newWhirl(int id,
				com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes, int bufferCap)
		{
			return new ThreadedWhirl<V>(id, target, maxNodes, bufferCap,
					new DefaultWhirlPolicy<V>());
		}
	},
	RoundRobin
	{
		@Override
		public <V extends Pointer<?>> Whirl<V> newWhirl(int id,
				com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes, int bufferCap)
		{
			return new RoundRobinThreadedWhirl<V>(id, bufferCap,
					new DefaultWhirlPolicy<V>());
		}
	},
	Simple
	{
		@Override
		public <V extends Pointer<?>> Whirl<V> newWhirl(int id,
				com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes, int bufferCap)
		{
			return new SimpleWhirl<V>(id, target, maxNodes, bufferCap,
					new DefaultWhirlPolicy<V>());
		}
	},
	Forked
	{
		@Override
		public <V extends Pointer<?>> Whirl<V> newWhirl(int id,
				com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes, int bufferCap)
		{
			return new ForkedWhirl<V>(id, target, maxNodes, bufferCap,
					new DefaultWhirlPolicy<V>());
		}
	};
	
	private Whirls()
	{
		return;
	}
	
	public abstract <V extends Pointer<?>> Whirl<V> newWhirl(int id,
			com.evalon.chrysalis.concurrent.Request<V, V> target, int maxNodes, int bufferCap);
}
