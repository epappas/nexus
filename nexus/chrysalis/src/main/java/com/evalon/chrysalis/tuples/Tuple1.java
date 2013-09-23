package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple1<A> implements TupleSet<A> {

	private final A a;

	public Tuple1(A a) {
		this.a = a;
	}

	@Override
	public A get() {
		return a;
	}

	@Override
	public TupleSet<?> left() {
		return null;
	}

	public TupleSet add(Object b) {
		return new Tuple2(a, b);
	}
}
