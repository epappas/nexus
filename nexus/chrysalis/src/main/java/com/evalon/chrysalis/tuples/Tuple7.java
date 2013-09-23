package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple7<A, B, C, D, E, F, G> implements TupleSet<G> {

	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;
	private final G g;

	public Tuple7(A a, B b, C c, D d, E e, F f, G g) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.g = g;
	}

	@Override
	public G get() {
		return g;
	}

	@Override
	public TupleSet<F> left() {
		return new Tuple6<>(a, b, c, d, e, f);
	}

	@Override
	public TupleSet add(Object h) {
		return new Tuple8(a, b, c, d, e, f, g, h);
	}
}
