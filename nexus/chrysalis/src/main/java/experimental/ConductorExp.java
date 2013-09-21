/**
 *
 */
package experimental;

import com.evalon.chrysalis.concurrent.Conductor;
import com.evalon.chrysalis.concurrent.Conductor.Tone;
import com.evalon.chrysalis.functional.ReceiveCallback;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/** @author Evangelos Pappas - Evalon.gr */
public class ConductorExp {

	/**
	 * @param args
	 *
	 * @author Evangelos Pappas - Evalon.gr
	 */
	public static void main(String[] args) {
		final Conductor conductor = new Conductor(5, 1024);
		conductor.start();
		final AtomicInteger integer = new AtomicInteger(0);

		conductor.conduct(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				if (integer.get() < 1000) {
					System.out.println("FROM "
							                   + Thread.currentThread().getName());
					System.out.println("Got " + integer.get());
					conductor.conduct(this);
					Thread.currentThread().sleep(5);
					return integer.incrementAndGet();
				}
				return 0;
			}
		}).onComplete(new ReceiveCallback<Conductor.Tone<?>, Integer>() {
			@Override
			public Tone<?> apply(Integer v) {
				System.out.println("Run " + v + " Loop!");
				conductor.stop();
				return null;
			}
		});

	}
}
