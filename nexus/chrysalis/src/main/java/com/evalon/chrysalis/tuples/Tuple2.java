package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple2<A, B> implements TupleSet<B> {

	private final A a;
	private final B b;

	public Tuple2(A a, B b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public B get() {
		return b;
	}

	@Override
	public TupleSet<A> left() {
		return new Tuple1<A>(a);
	}

	@Override
	public TupleSet add(Object c) {
		return new Tuple3(a, b, c);
	}
}
