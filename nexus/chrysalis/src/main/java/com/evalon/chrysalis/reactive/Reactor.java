package com.evalon.chrysalis.reactive;

import com.evalon.chrysalis.concurrent.Conductor;
import com.evalon.chrysalis.functional.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class Reactor {

	private final Conductor conductor;
	private final HashMap<String, Topic> map;

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

	public <V> Reactor emit(String topic, V... v) {
		// TODO
		return this;
	}

	public <V> Reactor emit(Topic topic, V... v) {
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

		private final Function onMessage;
		private final Function onError;
		private final Function onSubscribe;

		public DefaultSubscriber(Function onMsg) {
			this.onMessage = onMsg;
			this.onError = this.onSubscribe = new Function() {
				@Override
				public Object apply(Object o) {
					return null;
				}
			};
		}

		public DefaultSubscriber(Function onMsg, Function onErr) {
			this.onMessage = onMsg;
			this.onError = onErr;
			this.onSubscribe = new Function() {
				@Override
				public Object apply(Object o) {
					return null;
				}
			};
		}

		public DefaultSubscriber(Function onMsg, Function onErr, Function onSubs) {
			this.onMessage = onMsg;
			this.onError = onErr;
			this.onSubscribe = onSubs;
		}

		@Override
		public <R, V> R onMessage(V v) {
			return (R) this.onMessage.apply(v);
		}

		@Override
		public <V> void onError(V v) {
			this.onError.apply(v);
		}

		@Override
		public Subscriber onSubscribe(Topic topic) {
			this.onSubscribe.apply(topic);
			return this;
		}
	}

	protected class DefaultTopic implements Topic {
		private final String topic;
		private final Conductor conductor;
		private final List<Subscriber> subscribers;

		public DefaultTopic(String topic, Conductor conductor) {
			this.topic = topic;
			this.conductor = conductor;
			this.subscribers = new ArrayList<>();
		}

		@Override
		public <V> DefaultTopic publish(final V message) {
			final Iterator<Subscriber> iterator = this.subscribers.iterator();
			while (iterator.hasNext()) {
				final Subscriber s = iterator.next();
				this.conductor.conduct(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						s.onMessage(message);
						return null;
					}
				});
			}
			return this;
		}

		@Override
		public boolean subscribe(final Subscriber subscriber) {
			if (this.subscribers.add(subscriber)) {
				this.onSubscribe(subscriber);
				return true;
			}
			return false;
		}

		@Override
		public void onSubscribe(final Subscriber subscriber) {
			final DefaultTopic __this = this;
			this.conductor.conduct(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					subscriber.onSubscribe(__this);
					return null;
				}
			});
		}

		@Override
		public boolean start() {
			return false;
		}
	}

}
