package jnnet.draft;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static jnnet.DLLTools.loadDLL;
import static jnnet.draft.Neuron.sigmoid;
import static multij.tools.SystemProperties.getAvailableProcessorCount;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.getThisMethodName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import multij.tools.TicToc;

import org.junit.Test;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.amd.aparapi.Kernel.EXECUTION_MODE;

/**
 * @author codistmonk (creation 2014-03-02)
 */
public final class OpenCLTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
		double sum = 0.0;
		
		debugPrint(new Date(timer.tic()));
		
		for (int i = 0; i < N; ++i) {
			sum += f(i);
		}
		
		debugPrint(getThisMethodName(), "sum", sum, "time:", timer.toc());
	}
	
	@Test
	public final void test2() throws Exception {
		final int n = getAvailableProcessorCount();
		final ExecutorService executor = newFixedThreadPool(n);
		
		try {
			final TicToc timer = new TicToc();
			
			debugPrint(new Date(timer.tic()));
			
			final List<Future<?>> tasks = new ArrayList<Future<?>>(n);
			final int step = N / n;
			final double[] sums = new double[n];
			
			for (int i = 0; i < N; i += step) {
				final int start = i;
				
				tasks.add(executor.submit(new Runnable() {
					
					@Override
					public final void run() {
						final int taskId = start / step;
						final int end = min(N, start + step);
						
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
	
	@Test
	public final void test3() throws Exception {
		loadDLL("aparapi");
		
		final TicToc timer = new TicToc();
		
		debugPrint(new Date(timer.tic()));
		
		final int step = N / (1 << 11);
		final int n = (N + step - 1) / step;
		final double[] sums = new double[n];
		
		final Kernel kernel = new Kernel() {
			
			private final double[] weights = WEIGHTS;
			
			private final double[] sources = SOURCES;
			
			@Override
			public final void run() {
				final int globalId = this.getGlobalId();
				final int start = globalId * step;
				final int end = min(N, start + step);
				
				for (int i = start; i < end; ++i) {
					sums[globalId] += f(this.weights, this.sources, i);
				}
			}
			
		};
		
		try {
			final Range range = Range.create(n);
			
			debugPrint(range.getGlobalSize(0), range.getLocalSize(0),
					range.getWorkGroupSize(), range.getNumGroups(0));
			
			kernel.setExecutionMode(EXECUTION_MODE.GPU);
			kernel.execute(range);
			
			double sum = 0.0;
			
			for (final double value : sums) {
				sum += value;
			}
			
			debugPrint(getThisMethodName(), "sum", sum, "time:", timer.toc());
		} finally {
			kernel.dispose();
		}
	}
	
	public static final double f(final int i) {
		return f(WEIGHTS, SOURCES, i);
	}
	
	public static final double f(final double[] weights, final double[] sources, final int i) {
		// TODO find a better slowing trick...
		return exp(log(exp(log(exp(log(sigmoid(weights[i] * sources[i])))))));
	}
	
	public static final int N = 0x00FFF000;
	
	public static final double[] WEIGHTS = new double[N];
	
	public static final double[] SOURCES = new double[N];
	
	static {
		final Random random = new Random(0L);
		
		for (int i = 0; i < N; ++i) {
			WEIGHTS[i] = random.nextGaussian();
			SOURCES[i] = random.nextGaussian();
		}
		
		debugPrint("dataSize:", 2L * N * Double.SIZE / Byte.SIZE);
	}
	
}
