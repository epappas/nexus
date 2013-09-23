package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple3<A, B, C> implements TupleSet<C> {

	private final A a;
	private final B b;
	private final C c;

	public Tuple3(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	@Override
	public C get() {
		return c;
	}

	@Override
	public TupleSet<B> left() {
		return new Tuple2<>(a, b);
	}

	@Override
	public TupleSet add(Object d) {
		return new Tuple4(a, b, c, d);
	}
}
