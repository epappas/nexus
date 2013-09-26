package com.evalon.chrysalis.reactive;

public interface Topic {
	<V> Object publish(V message);

	boolean subscribe(Object node);

	boolean start();

}
