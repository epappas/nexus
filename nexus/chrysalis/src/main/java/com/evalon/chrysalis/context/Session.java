package com.evalon.chrysalis.context;

import com.evalon.chrysalis.functional.ReceiveCallback;
import com.evalon.chrysalis.functional.TraversedReceive;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <R>
 * @param <V>
 */
public interface Session<R, V> extends TraversedReceive<R, V>, Serializable,
		Map<Object, Object>
{
	public interface Credentials
	{
		
	}
	
	public Credentials getCredentials();
	
	public Object getSessionId();
	
	public long getExpirationTime();
	
	public <T> Object add(final String topic, final T msg);
	
	public <T> Object get(final String topic);
	
	public R apply(V v);
	
	public R onMessage(V v, ReceiveCallback<?, V> callback);
	
	public void onLogout();
	
	public boolean isUnbind();
}
