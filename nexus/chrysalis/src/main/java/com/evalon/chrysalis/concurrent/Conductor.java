/**
 *
 */
package com.evalon.chrysalis.concurrent;

import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/** @author Evangelos Pappas - Evalon.gr */
public class Conductor {
	private final Thread batons[];
	private boolean isWaiting[];
	private boolean isRunning;
	protected final Queue<Tone<?>> chord;

	public Conductor(int threadsNo, int capacity) {
		// chord = new ArrayQueue<Tone<?>>(capacity);
		chord = new ConcurrentLinkedQueue<Tone<?>>();
		batons = new Thread[threadsNo];
		isWaiting = new boolean[threadsNo];
		isRunning = false;
		for (int i = 0; i < threadsNo; ++i) {
			batons[i] = new Thread(new com.evalon.chrysalis.concurrent.Conductor.ReactionLogic(i, this), "Conductor-" + i);
			batons[i].setDaemon(false);
			isWaiting[i] = false;
		}
	}

	public <E> Tone<E> conduct(Callable<E> task) {
		return new Tone<E>(this, task, true);
	}

	public <E> Tone<E> conductAfter(Tone<?> tone, Callable<E> task) {
		Tone<?> temp = tone;
		while (temp.hasNext()) {
			temp = temp.next();
		}
		return temp.then(task);
	}

	public synchronized void start() {
		this.isRunning = true;
		for (Thread baton : batons) {
			baton.start();
		}
	}

	/**
	 * @param callable
	 * @param <E>
	 *
	 * @return Promise<E>
	 *
	 * @throws InterruptedException
	 */
	public <E> Tone<E> pledge(Callable<E> callable)
			throws InterruptedException {
		return new Promise<E>(this, callable).getTone();
	}

	/**
	 * @param callables
	 * @param <E>
	 *
	 * @return
	 *
	 * @throws InterruptedException
	 */
	public <E> Promise<E>[] pledgeAll(Callable<E> callables[])
			throws InterruptedException {
		@SuppressWarnings("unchecked")
		Promise<E> promises[] = new Promise[callables.length];

		for (int i = 0; i < callables.length; ++i) {
			promises[i] = new Promise<E>(this, callables[i]);
		}

		return promises;
	}

	public <E> E get(Promise<E> promise) throws Exception {
		return promise.get();
	}

	public synchronized void stop() {
		this.isRunning = false;
		this.unblock();
		for (int i = 0; i < batons.length; ++i) {
			try {
				batons[i].join();
			}
			catch (InterruptedException e) {

			}
		}
	}

	protected void unblock() {
		for (int i = 0; i < isWaiting.length; ++i) {
			if (isWaiting[i]) {
				LockSupport.unpark(batons[i]);
				isWaiting[i] = false;
			}
		}
	}

	protected void onBlock(int i) {
		isWaiting[i] = true;
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	/** @param <E>  */
	public class Tone<E> {
		private ReceiveCallback<Tone<?>, Exception> onError;
		private ReceiveCallback<Tone<?>, E> onComplete;
		private Conductor conductor;
		private Tone<?> next;
		private Callable<E> task;

		protected Tone(Conductor conductor, Callable<E> task, boolean isHead) {
			this(conductor, task, isHead, new ReceiveCallback<Tone<?>, E>() {
						@Override
						public Tone<?> apply(E v) {
							return null;
						}
					}, new ReceiveCallback<Tone<?>, Exception>() {
						@Override
						public Tone<?> apply(Exception v) {
							return null;
						}
					}
			);
		}

		protected Tone(Conductor conductor, Callable<E> task, boolean isHead,
		               ReceiveCallback<Tone<?>, E> complete,
		               ReceiveCallback<Tone<?>, Exception> error) {
			this.conductor = conductor;
			this.next = null;
			this.onError = error;
			this.onComplete = complete;
			this.task = task;
			if (isHead) {
				this.conductor.chord.offer(this);
				this.conductor.unblock();
			}
		}

		@SuppressWarnings("unchecked")
		public <EE> Tone<EE> then(Callable<EE> task) {
			return (Tone<EE>) (this.next = new Tone<EE>(this.conductor, task,
					                                           false));
		}

		public void call() throws Exception {
			this.onComplete.apply(this.task.call());
		}

		public Conductor ok() {
			return this.conductor;
		}

		protected boolean hasNext() {
			return this.next != null;
		}

		protected Tone<?> next() {
			return this.next;
		}

		protected ReceiveCallback<Tone<?>, Exception> getOnError() {
			return onError;
		}

		public Tone<E> onError(ReceiveCallback<Tone<?>, Exception> onError) {
			this.onError = onError;
			return this;
		}

		protected ReceiveCallback<Tone<?>, E> getOnComplete() {
			return onComplete;
		}

		public Tone<E> onComplete(ReceiveCallback<Tone<?>, E> onComplete) {
			this.onComplete = onComplete;
			return this;
		}
	}

	private class ReactionLogic implements Runnable {
		private Conductor conductor;
		private int id;

		public ReactionLogic(int id, Conductor conductor) {
			this.id = id;
			this.conductor = conductor;
		}

		@Override
		public void run() {
			while (this.conductor.isRunning) {
				Tone<?> lastTone = this.conductor.chord.poll();
				if (lastTone == null) {
//					this.conductor.onBlock(id);
					LockSupport.parkNanos(Thread.currentThread(), 2);
					continue;
				}

				do {
					try {
						lastTone.call();
					}
					catch (Exception e) {
						lastTone.onError.apply(e);
					}
					finally {
						if (lastTone.hasNext()) {
							this.conductor.chord.offer(lastTone.next());
						}
						lastTone = this.conductor.chord.poll();
					}
				}
				while (lastTone != null);
			}
		}
	}

	public class Promise<E> implements Future<E> {
		private Tone<E> myTone;
		private ConcurrentLinkedQueue<Thread> blockedQueue;
		private boolean isComplete;
		private volatile E result;
		private volatile Exception error;

		protected Promise(Conductor conductor, Callable<E> callable)
				throws InterruptedException {
			isComplete = false;
			blockedQueue = new ConcurrentLinkedQueue<Thread>();
			myTone = new Conductor.Tone<E>(conductor, callable, false)
					         .onComplete(new ReceiveCallback<Conductor.Tone<?>, E>() {
						         @Override
						         public Conductor.Tone<?> apply(E v) {
							         result = v;
							         isComplete = true;
							         unblockAll();
							         return myTone;
						         }
					         }).onError(
							                   new ReceiveCallback<Conductor.Tone<?>, Exception>() {
								                   @Override
								                   public Tone<?> apply(Exception v) {
									                   error = v;
									                   unblockAll();
									                   return myTone;
								                   }
							                   });
			conductor.chord.offer(myTone);
			conductor.unblock();
		}

		public Tone<E> getTone() {
			return this.myTone;
		}

		@Override
		public boolean cancel(boolean b) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return isComplete;
		}

		public E get() {
			while (result == null) {
				if (error != null) {
					this.notifyAll();
				}
				blockedQueue.add(Thread.currentThread());
				LockSupport.parkNanos(100);
			}
			unblockAll();
			return result;
		}

		@Override
		public E get(long l, TimeUnit timeUnit)
				throws InterruptedException, ExecutionException, TimeoutException {
			final long until = System.nanoTime() + timeUnit.toNanos(l);
			long before = System.nanoTime();
			while (result == null && until > System.nanoTime()) {
				if (error != null) {
					this.notifyAll();
				}
				blockedQueue.add(Thread.currentThread());
				LockSupport.parkNanos(until - before);
				before = System.nanoTime();
			}
			unblockAll();
			return result;
		}

		public void unblock(Thread thread) {
			LockSupport.unpark(thread);
			blockedQueue.remove(thread);
		}

		public void unblockAll() {
			Thread temT = blockedQueue.poll();
			while (temT != null) {
				LockSupport.unpark(temT);
				temT = blockedQueue.poll();
			}
		}
	}
}
