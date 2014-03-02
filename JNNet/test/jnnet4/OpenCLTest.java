package jnnet4;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static jnnet.Neuron.sigmoid;
import static net.sourceforge.aprog.tools.SystemProperties.getAvailableProcessorCount;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getThisMethodName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-02)
 */
public final class OpenCLTest {
	
	@Test
	public final void testSequential() {
		final TicToc timer = new TicToc();
		double sum = 0.0;
		
		debugPrint(new Date(timer.tic()));
		
		for (int i = 0; i < N; ++i) {
			sum += f(i);
		}
		
		debugPrint(getThisMethodName(), "sum", sum, "time:", timer.toc());
	}
	
	@Test
	public final void testThreadPool() throws Exception {
		final int n = getAvailableProcessorCount();
		final ExecutorService executor = newFixedThreadPool(n);
		
		try {
			final TicToc timer = new TicToc();
			
			debugPrint(new Date(timer.tic()));
			
			final List<Future<?>> tasks = new ArrayList<Future<?>>(n);
			final int step = N / n;
			final double[] sums = new double[n];
			
			for (int i = 0; i < N; i += step) {
				final int taskId = i / step;
				final int start = i;
				final int end = min(N, start + step);
				
				tasks.add(executor.submit(new Runnable() {
					
					@Override
					public final void run() {
						for (int i = start; i < end; ++i) {
							sums[taskId] += f(i);
						}
					}
					
				}));
			}
			
			double sum = 0.0;
			
			for (int i = 0; i < n; ++i) {
				tasks.get(i).get();
				sum += sums[i];
			}
			
			debugPrint(getThisMethodName(), "sum", sum, "time:", timer.toc());
		} finally {
			executor.shutdown();
		}
	}
	
	public static final double f(final int i) {
		// TODO find a better slowing trick...
		return exp(log(exp(log(exp(log(sigmoid(WEIGHTS[i] * SOURCES[i])))))));
	}
	
	public static final int N = 100000000;
	
	public static final double[] WEIGHTS = new double[N];
	
	public static final double[] SOURCES = new double[N];
	
	static {
		final Random random = new Random(0L);
		
		for (int i = 0; i < N; ++i) {
			WEIGHTS[i] = random.nextGaussian();
			SOURCES[i] = random.nextGaussian();
		}
	}
	
}
