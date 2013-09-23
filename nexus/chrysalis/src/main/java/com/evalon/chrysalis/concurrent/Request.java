package com.evalon.chrysalis.concurrent;

import com.evalon.chrysalis.functional.ReceiveCallback;
import com.evalon.chrysalis.functional.TraversedReceive;
import com.evalon.chrysalis.util.Disposable;

import java.util.concurrent.TimeUnit;

/**
 * @param <V>
 * @param <K>
 *
 * @author Evangelos Pappas - Evalon.gr
 */
public interface Request<K, V> extends TraversedReceive<K, V> {
	/**
	 * @param time
	 * @param unit
	 *
	 * @return
	 *
	 * @author Evangelos Pappas - Evalon.gr
	 */
	Request<K, V> setTimeout(final long time, final TimeUnit unit);

	/**
	 * @param broker
	 * @param msg
	 *
	 * @return
	 *
	 * @author Evangelos Pappas - Evalon.gr
	 */
	Disposable publish(final com.evalon.chrysalis.concurrent.whirls.Whirl<V> broker, final V msg);

	/**
	 *
	 */
	K apply(V message);

	/**
	 * @return
	 *
	 * @author Evangelos Pappas - Evalon.gr
	 */
	V getMessage();

	interface RunnableCallback<T, R> extends ReceiveCallback<T, R>, Runnable,
			                                         Disposable {
		/**
		 *
		 */
		T apply(final R message);

		/**
		 *
		 */
		void run();

		/**
		 *
		 */
		void dispose();

		/**
		 * @param broker
		 * @param msg
		 *
		 * @author Evangelos Pappas - Evalon.gr
		 */
		void send(final com.evalon.chrysalis.concurrent.whirls.Whirl<R> broker, final R msg);
	}
}
