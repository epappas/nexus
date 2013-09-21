/**
 * 
 */
package com.evalon.chrysalis.context;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public interface SessionHandler
{
	void onNewSession(Session<?, ?> session);
	
	void addSession(Session<?, ?> session);
	
	void expireSession(String sessionId);
}
