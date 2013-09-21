package com.evalon.chrysalis.context;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class SessionTopic<T> implements Topic
{
	private final String		topic;
	private final Session<T, T>	session;
	
	public SessionTopic(String topic, Session<T, T> session)
	{
		this.topic = topic;
		this.session = session;
	}
	
	public String getTopic()
	{
		return topic;
	}
	
	public <V> Object publish(V message)
	{
		return this.session.add(topic, message);
	}
	
	public boolean subscribe(Object node)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean start()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
