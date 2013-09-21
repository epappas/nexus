/**
 * 
 */
package experimental;

import com.evalon.chrysalis.concurrent.Conductor;
import com.evalon.chrysalis.concurrent.Conductor.Tone;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Evangelos Pappas - Evalon.gr
 */
public class SocketReactor
{
	private final Conductor									conductor;
	private final ServerSocketChannel						channel;
	private final InetSocketAddress							address;
	@SuppressWarnings("unused")
	private final Map<SocketChannel, List<byte[]>>			keepDataTrack;
	
	private Object											attachment	= null;
	private boolean											isRunning	= false;
	private ReceiveCallback<SocketReactor, SelectionKey>	onAccept;
	private ReceiveCallback<SocketReactor, SelectionKey>	onRead;
	private ReceiveCallback<SocketReactor, SelectionKey>	onWrite;
	private ReceiveCallback<SocketReactor, SelectionKey>	onConnect;
	private ReceiveCallback<SocketReactor, SelectionKey>	onInvalid;
	
	public SocketReactor(Conductor conductor, ServerSocketChannel ssc,
			InetSocketAddress isa) throws IOException
	{
		this.keepDataTrack = new ConcurrentHashMap<SocketChannel, List<byte[]>>();
		this.isRunning = false;
		this.conductor = conductor;
		this.address = isa;
		this.channel = ssc;
		this.channel.configureBlocking(false);
	}
	
	public SocketReactor(Conductor conductor, ServerSocketChannel ssc)
			throws IOException
	{
		this(conductor, ssc, null);
	}
	
	public SocketReactor(Conductor conductor, String host, int port)
			throws IOException
	{
		this(conductor, ServerSocketChannel.open(), new InetSocketAddress(host,
				port));
	}
	
	public SocketReactor start() throws IOException
	{
		this.channel.socket().bind(this.address);
		this.isRunning = true;
		this.start0();
		if (!conductor.isRunning())
		{
			throw new RuntimeException("Conductor is not running");
		}
		return this;
	}
	
	public SocketReactor setOption(SocketOption<Integer> name, Integer value)
			throws IOException
	{
		this.channel.setOption(name, value);
		return this;
	}
	
	private void start0()
	{
		try
		{
			final Selector selector = Selector.open();
			this.channel.register(selector, SelectionKey.OP_ACCEPT, attachment);
			
			this.conductor.conduct(new Callable<Boolean>()
			{
				@Override
				public Boolean call() throws Exception
				{
					if (isRunning)
					{						
						//if (selector.select(100) == 0)
						if (selector.selectNow() == 0)
						{
							conductor.conduct(this);
							return isRunning;
						}
						
						Set<SelectionKey> keys = selector.selectedKeys();
						Iterator<SelectionKey> iterator = keys.iterator();
						
						while (iterator.hasNext())
						{
							final SelectionKey key = iterator.next();
							iterator.remove();
							conductor
									.conduct(new Callable<SelectionKey>()
									{
										@Override
										public SelectionKey call()
												throws Exception
										{
											if (!key.isValid())
											{
												onInvalid.apply(key);
											}
											if (key.isAcceptable())
											{
												onAccept.apply(key);
											}
											else if (key.isReadable())
											{
												onRead.apply(key);
											}
											else if (key.isWritable())
											{
												onWrite.apply(key);
											}
											else if (key.isConnectable())
											{
												onConnect.apply(key);
											}
											return key;
										}
									})
									.onComplete(
											new ReceiveCallback<Conductor.Tone<?>, SelectionKey>()
											{
												
												@Override
												public Tone<?> apply(
														                                                                    SelectionKey v)
												{
													return null;
												}
											});
						}
						conductor.conduct(this);
					}
					return isRunning;
				}
			}).onComplete(new ReceiveCallback<Conductor.Tone<?>, Boolean>()
			{
				@Override
				public Tone<?> apply(Boolean v)
				{
					return null;
				}
			});
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public Object getAttachment()
	{
		return this.attachment;
	}
	
	public SocketReactor setAttachment(Object attachment)
	{
		this.attachment = attachment;
		return this;
	}
	
	public SocketReactor onAccept(
			ReceiveCallback<SocketReactor, SelectionKey> callback)
	{
		this.onAccept = callback;
		return this;
	}
	
	public SocketReactor onConnect(
			ReceiveCallback<SocketReactor, SelectionKey> callback)
	{
		this.onConnect = callback;
		return this;
	}
	
	public SocketReactor onRead(
			ReceiveCallback<SocketReactor, SelectionKey> callback)
	{
		this.onRead = callback;
		return this;
	}
	
	public SocketReactor onWrite(
			ReceiveCallback<SocketReactor, SelectionKey> callback)
	{
		this.onWrite = callback;
		return this;
	}
	
	public SocketReactor onInvalid(
			ReceiveCallback<SocketReactor, SelectionKey> callback)
	{
		this.onInvalid = callback;
		return this;
	}
}
