package com.evalon.chrysalis.reactive;

import com.evalon.chrysalis.concurrent.Conductor;

public class Reactor {

	private final Conductor conductor;

	public Reactor(Conductor conductor) {
		this.conductor = conductor;

	}

	public <E> F<E> __(E... es) {
		return new F<E>(this.conductor, es);
	}

	public <E> F<E> __(Iterable<E> iterable) {
		return new F<E>(this.conductor, iterable);
	}

}
