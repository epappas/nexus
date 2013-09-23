package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple6<A, B, C, D, E, F> implements TupleSet<F> {

	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;

	public Tuple6(A a, B b, C c, D d, E e, F f) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
	}

	@Override
	public F get() {
		return f;
	}

	@Override
	public TupleSet<E> left() {
		return new Tuple5<>(a, b, c, d, e);
	}

	@Override
	public TupleSet add(Object g) {
		return new Tuple7(a, b, c, d, e, f, g);
	}
}
