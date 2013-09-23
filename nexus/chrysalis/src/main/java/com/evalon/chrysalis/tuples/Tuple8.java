package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple8<A, B, C, D, E, F, G, H> implements TupleSet<H> {

	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;
	private final G g;
	private final H h;

	public Tuple8(A a, B b, C c, D d, E e, F f, G g, H h) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.g = g;
		this.h = h;
	}

	@Override
	public H get() {
		return h;
	}

	@Override
	public TupleSet<F> left() {
		return new Tuple6<>(a, b, c, d, e, f);
	}

	@Override
	public TupleSet add(Object o) {
		return null;
	}
}
