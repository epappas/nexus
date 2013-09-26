package com.evalon.chrysalis.reactive;

/** User: Evangelos */
public interface Subscriber {

	public <V> V onMessage(V v);

	public <V> void onError(V v);

	public Subscriber onSubscribe(Topic topic);
}
