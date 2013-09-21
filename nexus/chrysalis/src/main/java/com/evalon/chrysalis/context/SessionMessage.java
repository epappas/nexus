/**
 * 
 */
package com.evalon.chrysalis.context;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class SessionMessage<T>
{
	private final T			msg;
	private final String	topic;
	
	public SessionMessage(String topic, T msg)
	{
		this.topic = topic;
		this.msg = msg;
	}
	
	public String getTopic()
	{
		return topic;
	}
	
	public T getMessage()
	{
		return msg;
	}
}
