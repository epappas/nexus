package com.evalon.chrysalis.reactive;

public interface Topic {
	<V> Topic publish(V message);

	boolean subscribe(final Subscriber subscriber);

	void onSubscribe(Subscriber subscriber);

	boolean start();

}
