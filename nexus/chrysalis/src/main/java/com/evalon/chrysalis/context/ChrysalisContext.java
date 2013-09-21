/**
 * 
 */
package com.evalon.chrysalis.context;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class ChrysalisContext extends ApplicationContext
{
	private ProcessorContext	processorContext;
	
	/**
	 * @return the processorContext
	 */
	public ProcessorContext getProcessorContext()
	{
		return processorContext;
	}
	
	/**
	 * @param processorContext
	 *            the processorContext to set
	 */
	protected void setProcessorContext(ProcessorContext processorContext)
	{
		this.processorContext = processorContext;
	}
	
	public ClassLoader getClassLoader()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onNewSession(Session<?, ?> session)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addSession(Session<?, ?> session)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void expireSession(String sessionId)
	{
		// TODO Auto-generated method stub
		
	}
}
