package jnnet3.dct.draft;

import static java.lang.Math.PI;
import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.sin;
import static multij.tools.Tools.cartesian;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-09)
 */
public final class DWT {
	
	private DWT() {
		throw new IllegalInstantiationException();
	}
	
	public static final int sum(final int... values) {
		return Arrays.stream(values).sum();
	}
	
	public static final int product(final int... values) {
		return Arrays.stream(values).reduce(1, (x, y) -> x * y);
	}
	
	public static final double waveCell(final IntFunction<DoubleUnaryOperator> waveFactory, final double[] xs, final int[] ks) {
		final int m = xs.length;
		final int n = 1 + sum(ks);
		final DoubleUnaryOperator wave = waveFactory.apply(n);
		final int count = 1 << (m - 1);
		double result = 0.0;
		
		for (int i = 0; i < count; ++i) {
			double argument = ks[0] * xs[0] / n;
			
			for (int j = 1; j < m; ++j) {
				final int signum = ((i >> (j - 1)) & 1) * 2 - 1;
				argument += signum * ks[j] * xs[j] / n;
			}
			
			result += wave.applyAsDouble(argument);
		}
		
		return result;
	}
	
	public static final BasicMatrix newWaveColumn(final IntFunction<DoubleUnaryOperator> waveFactory, final int[] ns, final double[] xs) {
		final int n = product(ns);
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		int i = -1;
		
		for (final int[] ks : cartesian(bounds(ns))) {
			builder.set(++i, 0, waveCell(waveFactory, xs, ks));
		}
		
		return builder.build();
		
	}
	
	public static final BasicMatrix newIDWTMatrix(final IntFunction<DoubleUnaryOperator> waveFactory, final int[] ns) {
		final int n = product(ns);
		final double[] normalizedXs = new double[ns.length];
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, n);
		final Iterable<int[]> tuples = cartesian(bounds(ns));
		int i = -1;
		
		for (final int[] xs : tuples) {
			++i;
			int j = -1;
			
			normalize(xs, ns, normalizedXs);
			
			for (final int[] ks : tuples) {
				++j;
				
				builder.set(i, j, waveCell(waveFactory, normalizedXs, ks));
			}
		}
		
		return builder.build();
	}
	
	public static final void normalize(final int[] input, final int[] ns, final double[] output) {
		final int n = input.length;
		
		for (int i = 0; i < n; ++i) {
			output[i] = (double) input[i] / ns[i];
		}
	}
	
	public static final int[] bounds(final int... sizes) {
		final int n = sizes.length;
		final int[] result = new int[2 * n];
		
		for (int i = 0; i < n; ++i) {
			result[2 * i + 1] = sizes[i] - 1;
		}
		
		return result;
	}
	
	public static final double relu(final double x) {
		return max(0.0, x);
	}
	
	public static final double sigmoid(final double x) {
		return 1.0 / (1.0 + exp(-x));
	}
	
	public static final double sinmoid(final double x) {
		return x < -PI / 2.0 ? -1.0 : x < PI / 2.0 ? sin(x) : 1.0;
	}
	
	public static final double step(final double x) {
		return x < 0.0 ? 0.0 : 1.0;
	}
	
	/**
	 * @author codistmonk (creation 2015-07-09)
	 */
	public static final class SinmoidWave implements Serializable, DoubleUnaryOperator {
		
		private final double n;
		
		public SinmoidWave(final double n) {
			this.n = n;
		}
		
		@Override
		public final double applyAsDouble(final double operand) {
			double value = 1.0 - sinmoid(PI * (this.n * operand - 0.5));
			final int n = (int) ceil(this.n);
			
			for (int i = 1, signum = 1; i < n; ++i, signum = -signum) {
				value += signum * (1.0 + sinmoid(PI * (this.n * operand - 0.5 - i)));
			}
			
			return value / 2.0;
		}
		
		private static final long serialVersionUID = 824983855310692902L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-07-09)
	 */
	public static final class StepWave implements Serializable, DoubleUnaryOperator {
		
		private final double n;
		
		public StepWave(final double n) {
			this.n = n;
		}
		
		@Override
		public final double applyAsDouble(final double operand) {
			double result = 1.0 - step(this.n * operand - 0.5);
			final int n = (int) ceil(this.n);
			
			for (int i = 1, signum = 1; i < n; ++i, signum = -signum) {
				result += signum * step(this.n * operand - 0.5 - i);
			}
			
			return result;
		}
		
		private static final long serialVersionUID = -2587273024895480429L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-07-09)
	 */
	public static final class ReLUWave implements Serializable, DoubleUnaryOperator {
		
		private final double n;
		
		public ReLUWave(final double n) {
			this.n = n;
		}
		
		@Override
		public final double applyAsDouble(final double operand) {
			double result = 1.0 - relu(this.n * operand);
			final int n = (int) ceil(this.n);
			
			for (int i = 1, signum = 2; i < n; ++i, signum = -signum) {
				result += signum * relu(this.n * operand - i);
			}
			
			return result;
		}
		
		private static final long serialVersionUID = -2587273024895480429L;
		
	}
	
}
