package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public class T extends Tuple {
	public <A> T(A a) throws Exception {
		super(a);
	}

	public <A, B> T(A a, B b) throws Exception {
		super(a, b);
	}

	public <A, B, C> T(A a, B b, C c) throws Exception {
		super(a, b, c);
	}

	public <A, B, C, D> T(A a, B b, C c, D d) throws Exception {
		super(a, b, c, d);
	}

	public <A, B, C, D, E> T(A a, B b, C c, D d, E e) throws Exception {
		super(a, b, c, d, e);
	}

	public <A, B, C, D, E, F> T(A a, B b, C c, D d, E e, F f) throws Exception {
		super(a, b, c, d, e, f);
	}

	public <A, B, C, D, E, F, G> T(A a, B b, C c, D d, E e, F f, G g) throws Exception {
		super(a, b, c, d, e, f, g);
	}

	public <A, B, C, D, E, F, G, H> T(A a, B b, C c, D d, E e, F f, G g, H h) throws Exception {
		super(a, b, c, d, e, f, g, h);
	}
}
