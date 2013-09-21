/**
 * 
 */
package com.evalon.chrysalis.context;

import java.util.List;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public abstract class ApplicationContext implements Context, SessionHandler
{
	private List<Session<?, ?>>	sessions;
	
	public abstract void onNewSession(Session<?, ?> session);
	
	public abstract void addSession(Session<?, ?> session);
	
	public abstract void expireSession(String sessionId);
}
