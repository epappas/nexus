package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple4<A, B, C, D> implements TupleSet<D> {

	private final A a;
	private final B b;
	private final C c;
	private final D d;

	public Tuple4(A a, B b, C c, D d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	@Override
	public D get() {
		return d;
	}

	@Override
	public TupleSet<C> left() {
		return new Tuple3<>(a, b, c);
	}

	@Override
	public TupleSet add(Object e) {
		return new Tuple5(a, b, c, d, e);
	}
}
