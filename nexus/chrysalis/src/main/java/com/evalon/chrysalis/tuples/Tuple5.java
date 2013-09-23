package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple5<A, B, C, D, E> implements TupleSet<E> {

	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;

	public Tuple5(A a, B b, C c, D d, E e) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}

	@Override
	public E get() {
		return e;
	}

	@Override
	public TupleSet<D> left() {
		return new Tuple4<>(a, b, c, d);
	}

	@Override
	public TupleSet add(Object f) {
		return new Tuple6(a, b, c, d, e, f);
	}
}
