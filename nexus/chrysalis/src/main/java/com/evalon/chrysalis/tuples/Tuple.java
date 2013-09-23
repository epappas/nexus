package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class Tuple implements TupleSet {

	private final TupleSet tupleSet;

	public <A> Tuple(A a) throws Exception {
		tupleSet = new Tuple1(a);
	}

	public <A, B> Tuple(A a, B b) throws Exception {
		tupleSet = new Tuple2(a, b);
	}

	public <A, B, C> Tuple(A a, B b, C c) throws Exception {
		tupleSet = new Tuple3(a, b, c);
	}

	public <A, B, C, D> Tuple(A a, B b, C c, D d) throws Exception {
		tupleSet = new Tuple4(a, b, c, d);
	}

	public <A, B, C, D, E> Tuple(A a, B b, C c, D d, E e) throws Exception {
		tupleSet = new Tuple5(a, b, c, d, e);
	}

	public <A, B, C, D, E, F> Tuple(A a, B b, C c, D d, E e, F f) throws Exception {
		tupleSet = new Tuple6(a, b, c, d, e, f);
	}

	public <A, B, C, D, E, F, G> Tuple(A a, B b, C c, D d, E e, F f,
	                                   G g) throws Exception {
		tupleSet = new Tuple7(a, b, c, d, e, f, g);
	}

	public <A, B, C, D, E, F, G, H> Tuple(A a, B b, C c, D d, E e, F f,
	                                      G g, H h) throws Exception {
		tupleSet = new Tuple8(a, b, c, d, e, f, g, h);
	}

	@Override
	public Object get() {
		return this.tupleSet.get();
	}

	@Override
	public TupleSet left() {
		return this.tupleSet.left();
	}

	@Override
	public TupleSet add(Object o) {
		return this.tupleSet.add(o);
	}
}
