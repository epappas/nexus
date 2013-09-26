package com.evalon.chrysalis.reactive;

import com.evalon.chrysalis.concurrent.Conductor;
import com.evalon.chrysalis.functional.Function;

import java.util.HashMap;
import java.util.List;

public class Reactor {

	private final Conductor conductor;
	private final HashMap<Topic, List<Subscriber>> map;

	public Reactor(Conductor conductor) {
		this.conductor = conductor;
		this.map = new HashMap<>();
	}

	public <E> F<E> __(E... es) {
		return new F<E>(this.conductor, es);
	}

	public <E> F<E> __(Iterable<E> iterable) {
		return new F<E>(this.conductor, iterable);
	}

	public <V> Reactor emit(String topic, V v) {
		// TODO
		return this;
	}

	public <V> Reactor emit(Topic topic, V v) {
		// TODO
		return this;
	}

	public Reactor on(String topic, Function fn) {
		// TODO
		return this;
	}

	public Reactor on(String topic, Subscriber subscriber) {
		// TODO
		return this;
	}

	public Reactor on(Topic topic, Function fn) {
		// TODO
		return this;
	}

	public Reactor on(Topic topic, Subscriber subscriber) {
		// TODO
		return this;
	}

	protected class DefaultSubscriber implements Subscriber {

		@Override
		public <V> V onMessage(V v) {
			return null;
		}

		@Override
		public <V> void onError(V v) {

		}

		@Override
		public Subscriber onSubscribe(Topic topic) {
			return null;
		}
	}

	protected class DefaultTopic implements Topic {

		@Override
		public <V> Object publish(V message) {
			return null;
		}

		@Override
		public boolean subscribe(Object node) {
			return false;
		}

		@Override
		public boolean start() {
			return false;
		}
	}

}
