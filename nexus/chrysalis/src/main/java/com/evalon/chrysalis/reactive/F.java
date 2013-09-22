package com.evalon.chrysalis.reactive;

import com.evalon.chrysalis.concurrent.Conductor;
import com.evalon.chrysalis.functional.Consumer;
import com.evalon.chrysalis.functional.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class F<E> {

	private final Conductor conductor;
	private final ArrayList<E> arrayList;

	public F(Conductor conductor, E... es) {
		this.conductor = conductor;
		this.arrayList = new ArrayList<E>();
		Collections.addAll(this.arrayList, es);
	}

	public F(Conductor conductor, Iterable<E> iterable) {
		this.conductor = conductor;
		this.arrayList = new ArrayList<E>();
		for (E anIterable : iterable) {
			this.arrayList.add(anIterable);
		}
	}

	public F(Conductor conductor, int from, int to) {
		this.conductor = conductor;
		this.arrayList = new java.util.ArrayList<E>();
		for (int i = from; i < to; ++i) {
			this.arrayList.add((E) new Integer(i));
		}
	}

	public F<E> reverse() {
		final ArrayList<E> list = new ArrayList<>();

		for (int i = this.arrayList.size() - 1; i >= 0; --i) {
			list.add(this.arrayList.get(i));
		}
		return new F<E>(this.conductor, list);
	}

	public F<E> takeLast(int n) {
		final ArrayList<E> list = new ArrayList<>();
		final int size = this.arrayList.size();
		for (int i = size - (1 + n); i < size; ++i) {
			list.add(this.arrayList.get(i));
		}
		return new F<E>(this.conductor, list);
	}

	public F<E> takeFirst(int n) {
		final ArrayList<E> list = new ArrayList<>();
		final int size = this.arrayList.size();
		for (int i = 0; i < n && i < size; ++i) {
			list.add(this.arrayList.get(i));
		}
		return new F<E>(this.conductor, list);
	}

	public F<E> take(int from, int to) {
		final ArrayList<E> list = new ArrayList<>();
		final int size = this.arrayList.size();
		for (int i = from; i < to && i < size; ++i) {
			list.add(this.arrayList.get(i));
		}
		return new F<E>(this.conductor, list);
	}

	public Conductor.Tone<ArrayList<E>> first(final Function<E, E> function) {
		final ArrayList<E> list = new ArrayList<>();
		return conductor.conduct(new Callable<ArrayList<E>>() {
			@Override
			public ArrayList<E> call() throws Exception {
				function.apply(arrayList.get(0));
				return (ArrayList<E>) take(1, arrayList.size()).toList();
			}
		});
	}

	public Conductor.Tone<ArrayList<E>> last(final Function<E, E> function) {
		final ArrayList<E> list = new ArrayList<>();
		return conductor.conduct(new Callable<ArrayList<E>>() {
			@Override
			public ArrayList<E> call() throws Exception {
				function.apply(arrayList.get(arrayList.size() - 1));
				return (ArrayList<E>) take(0, arrayList.size() - 1).toList();
			}
		});
	}

	public Conductor.Tone<ArrayList<E>> map(final Function<E, E> function) {
		final ArrayList<E> list = new ArrayList<>();
		return conductor.conduct(new Callable<ArrayList<E>>() {
			@Override
			public ArrayList<E> call() throws Exception {
				Iterator<E> it = arrayList.iterator();
				while (it.hasNext()) {
					list.add(function.apply(it.next()));
				}
				return list;
			}
		});
	}

	public Conductor.Tone<ArrayList<E>> every(final Function<Boolean, E> function) {
		final ArrayList<E> list = new ArrayList<>();
		return conductor.conduct(new Callable<ArrayList<E>>() {
			@Override
			public ArrayList<E> call() throws Exception {
				Iterator<E> it = arrayList.iterator();
				while (it.hasNext()) {
					E tmp = it.next();
					if (function.apply(tmp)) {
						list.add(tmp);
					}
					else {
						return list;
					}
				}
				return list;
			}
		});
	}

	public Conductor.Tone<ArrayList<E>> filter(final Function<Boolean, E> function) {
		final ArrayList<E> list = new ArrayList<>();
		return conductor.conduct(new Callable<ArrayList<E>>() {
			@Override
			public ArrayList<E> call() throws Exception {
				Iterator<E> it = arrayList.iterator();
				while (it.hasNext()) {
					E tmp = it.next();
					if (function.apply(tmp)) {
						list.add(tmp);
					}
				}
				return list;
			}
		});
	}

	public Conductor.Tone<ArrayList<E>> forEach(final Consumer<E> consumer) {
		return conductor.conduct(new Callable<ArrayList<E>>() {
			@Override
			public ArrayList<E> call() throws Exception {
				Iterator<E> it = arrayList.iterator();
				while (it.hasNext()) {
					consumer.accept(it.next());
				}
				return arrayList;
			}
		});
	}

	public java.util.List<E> toList() {
		return this.arrayList;
	}
}
