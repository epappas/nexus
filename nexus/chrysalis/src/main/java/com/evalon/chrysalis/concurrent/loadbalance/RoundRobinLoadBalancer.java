/**
 *
 */
package com.evalon.chrysalis.concurrent.loadbalance;

import com.evalon.chrysalis.concurrent.Request;
import com.evalon.chrysalis.functional.Callback;
import com.evalon.chrysalis.memory.direct.DirectIntCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** @author Evangelos Pappas - Evalon.gr */
public class RoundRobinLoadBalancer<E extends Request<?, V>, V> implements
		LoadBalancer<E, V> {
	@SuppressWarnings("unchecked")
	private AtomicReference<E> handlers[] = new AtomicReference[4];
	private DirectIntCursor cursor = new DirectIntCursor(0, 4);
	private DirectIntCursor insPos = new DirectIntCursor(0, 4);
	private int size = 4;

	public RoundRobinLoadBalancer() {

	}

	public E serve(V block) {
		int k = 0;
		E e = null;
		do {
			k = this.cursor.incrementAndGet();
			if (handlers[k] != null) {
				e = handlers[k].get();
			}
		}
		while (e == null);
		e.apply(block);
		return e;
	}

	public E serve(V block, Callback<?, V> callback) {
		int k = 0;
		E e = null;
		do {
			k = this.cursor.incrementAndGet();
			if (handlers[k] != null) {
				e = handlers[k].get();
			}
		}
		while (e == null);
		e.apply(block, callback);
		return e;
	}

	public E serve(int nodeID, V block, Callback<?, V> callback) {
		E e = null;
		if (handlers[nodeID] != null) {
			e = handlers[nodeID].get();
			e.apply(block, callback);
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	public RoundRobinLoadBalancer<E, V> subscribe(E handler) {
		synchronized (this.handlers) {
			if ((this.insPos.get() + 1) >= this.size) {
				++this.size;
				this.cursor.setMax(this.size);
				this.insPos.setMax(this.size);
				AtomicReference<E> tmpHandlers[] = this.handlers;
				this.handlers = new AtomicReference[this.size];
				System.arraycopy(tmpHandlers, 0, this.handlers, 0,
						                tmpHandlers.length);
			}
			this.handlers[this.insPos.getAndIncrement()] = new AtomicReference<E>(
					                                                                     handler);
		}
		return this;
	}

	public List<E> listSubscribers() {
		ArrayList<E> list = new ArrayList<E>();
		synchronized (this.handlers) {
			for (AtomicReference<E> reference : this.handlers) {
				list.add(reference.get());
			}
		}
		return list;
	}

	public void close() {
		synchronized (this.handlers) {
			for (AtomicReference<E> reference : this.handlers) {
				reference.set(null);
			}
		}
	}

	public synchronized int getCursor() {
		return this.cursor.get();
	}

	public synchronized int getSize() {
		return this.size;
	}

	@Override
	public E apply(V v) {
		return this.serve(v);
	}
}
