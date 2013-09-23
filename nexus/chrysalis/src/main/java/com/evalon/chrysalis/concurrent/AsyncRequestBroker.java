package com.evalon.chrysalis.concurrent;

import com.evalon.chrysalis.util.Disposable;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Evangelos Pappas - Evalon.gr
 * @param <V>
 */
public abstract class AsyncRequestBroker<V> implements com.evalon.chrysalis.concurrent.whirls.TimedOutRequest<V, V>,
		Broker<Disposable, V>
{
	private final ReceiveCallback<V, V>		onTimedout;
	private final List<AtomicReference<V>>	inbox;
	private long							time;				// in Nanos
	private List<AsyncRunnableCallback>		runnableCallbacks;
	private ExecutorService					executor;

	/**
	 */
	public AsyncRequestBroker()
	{
		this.executor = Executors.newSingleThreadExecutor();
		this.inbox = new LinkedList<AtomicReference<V>>();
		this.runnableCallbacks = new LinkedList<AsyncRequestBroker<V>.AsyncRunnableCallback>();
		this.time = 0;
		this.onTimedout = new ReceiveCallback<V, V>()
		{
			public V apply(V v)
			{
				return AsyncRequestBroker.this.onTimedOut(v);
			}
		};
	}

	public AsyncRequestBroker<V> setTimeout(final long time, final TimeUnit unit)
	{
		this.time = TimeUnit.NANOSECONDS.convert(time, unit);
		return this;
	}

	public Disposable publish(final com.evalon.chrysalis.concurrent.whirls.Whirl<V> whirl, final V msg)
	{
		final AsyncRunnableCallback callback = new AsyncRunnableCallback(
				new ReceiveCallback<V, AsyncRunnableCallback>()
				{
					@SuppressWarnings("finally")
					public V apply(AsyncRunnableCallback runnableCallback)
					{
						runnableCallbacks.add(runnableCallback);
						try
						{
							executor.submit(runnableCallback, com.evalon.chrysalis.concurrent.whirls.Whirl.EMPTY).get(
									time, TimeUnit.NANOSECONDS);
						}
						finally
						{
							return null;
						}
					}
				}, this.onTimedout,
				new ReceiveCallback<V, AsyncRunnableCallback>()
				{
					public V apply(AsyncRunnableCallback runnableCallback)
					{
						runnableCallback.dispose();
						runnableCallbacks.remove(runnableCallback);
						return null;
					}

				});
		callback.send(whirl, msg);
		return callback;
	}

	public abstract V apply(V message);

	public abstract V onTimedOut(V message);

	/**
	 * @author Evangelos Pappas - Evalon.gr
	 */
	final private class AsyncRunnableCallback implements RunnableCallback<V, V>
	{
		private Disposable										d;
		private final ReceiveCallback<V, V>						onTimedout;
		private final ReceiveCallback<V, AsyncRunnableCallback>	onMessage;
		private final ReceiveCallback<V, AsyncRunnableCallback>	onComplete;
		private AtomicReference<V>								lastMsg;
		private com.evalon.chrysalis.concurrent.whirls.Whirl<V> whirl;

		public AsyncRunnableCallback(
				final ReceiveCallback<V, AsyncRunnableCallback> onMessage,
				final ReceiveCallback<V, V> onTimedout,
				ReceiveCallback<V, AsyncRunnableCallback> onComplete)
		{
			this.lastMsg = new AtomicReference<V>();
			this.onMessage = onMessage;
			this.onComplete = onComplete;
			this.onTimedout = onTimedout;
		}

		public V apply(final V message)
		{
			lastMsg.lazySet(message);
			AsyncRequestBroker.this.inbox.add(this.lastMsg);
			return this.onMessage.apply(this);
		}

		public void run()
		{
			try
			{
				whirl.publish(lastMsg.get());
			}
			catch (Exception e)
			{
				this.onTimedout.apply(lastMsg.get());
			}
			finally
			{
				this.onComplete.apply(this);
			}
		}

		public void dispose()
		{
			if (this.d != null)
			{
				inbox.remove(this.lastMsg);
			}
			try
			{
				this.finalize();
			}
			catch (Throwable e)
			{

			}
		}

		public void send(final com.evalon.chrysalis.concurrent.whirls.Whirl<V> whirl, final V msg)
		{
			this.whirl = whirl;
			this.apply(msg);
		}
	}

	public V getMessage()
	{
		try
		{
			return inbox.get(inbox.size() - 1).get();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
