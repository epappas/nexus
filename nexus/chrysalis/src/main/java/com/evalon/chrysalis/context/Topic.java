package com.evalon.chrysalis.context;

public interface Topic
{
	<V> Object publish(V message);
	
	boolean subscribe(Object node);
	
	boolean start();
	
}
