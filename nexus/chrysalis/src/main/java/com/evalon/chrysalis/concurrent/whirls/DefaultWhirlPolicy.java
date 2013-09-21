/**
 *
 */
package com.evalon.chrysalis.concurrent.whirls;

import com.evalon.chrysalis.concurrent.whirls.Whirl.BucketMessage;
import com.evalon.chrysalis.concurrent.whirls.Whirl.Reference;
import com.evalon.chrysalis.functional.Callback;
import com.evalon.chrysalis.util.Disposable;

/** @author Evangelos Pappas - Evalon.gr */
public class DefaultWhirlPolicy<V> implements WhirlPolicy<V> {
	private Whirl<V> whirl;

	public DefaultWhirlPolicy() {
	}

	protected DefaultWhirlPolicy(Whirl<V> broker) {
		this.whirl = broker;
	}

	public Whirl<V> getTarget() {
		return this.whirl;
	}

	public int hashIntPolicy() {
		long id = Thread.currentThread().getId();
		int hash = (((int) (id ^ (id >>> 32))) ^ 0x811c9dc5) * 0x01000193;

		int m = this.whirl.getMaxCapacity();
		int nbits = (((0xfffffc00 >> m) & 4) | // Compute ceil(log2(m+1))
				             ((0x000001f8 >>> m) & 2) | // The constants hold
				             ((0xffff00f2 >>> m) & 1)); // a lookup table
		int index;
		while ((index = hash & ((1 << nbits) - 1)) > m)// May retry on
		{
			hash = (hash >>> nbits) | (hash << (33 - nbits)); // non-power-2 m
		}
		return index;
	}

	public void setTarget(Whirl<V> broker) {
		this.whirl = broker;
	}

	@SuppressWarnings("unchecked")
	public com.evalon.chrysalis.concurrent.whirls.Whirl.BucketMessage<V> getBucketGeneration() {
		return (com.evalon.chrysalis.concurrent.whirls.Whirl.BucketMessage<V>) Whirl.EMPTY;
	}

	@SuppressWarnings("unchecked")
	public <T extends com.evalon.chrysalis.concurrent.Request<V, V>> T getNodeGeneration() {
		return (T) new DummyAsyncRequest();
	}

	public <T extends com.evalon.chrysalis.concurrent.Request<V, V>> void unsubscribe(com.evalon.chrysalis.concurrent.whirls.Whirl.Reference<T> reference) {
		// TODO Auto-generated method stub
	}

	private class DummyAsyncRequest extends com.evalon.chrysalis.concurrent.AsyncRequestBroker<V> {
		public DummyAsyncRequest() {
			super();
		}

		@Override
		public V apply(V message) {
			return message;
		}

		@Override
		public V onTimedOut(V message) {
			return message;
		}

		public Disposable publish(Whirl<V> whirl, V msg) {
			return new Disposable() {
				public void dispose() {

				}
			};
		}

		public V apply(V message, Callback<?, V> callback) {
			callback.apply(message);
			return message;
		}
	}
}
