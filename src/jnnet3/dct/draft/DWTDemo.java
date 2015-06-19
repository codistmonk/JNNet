package jnnet3.dct.draft;

import static java.lang.Math.*;
import static multij.tools.Tools.*;

import java.util.Arrays;

import multij.tools.IllegalInstantiationException;

import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;

/**
 * @author codistmonk (creation 2015)
 */
public final class DWTDemo {
	
	private DWTDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		demo1D();
		demo2D();
		demoND();
	}
	
	public static final void demo1D() {
		final BasicMatrix f = newColumn(1.0, 2.0, -3.0);
		final int n = f.size();
		final BasicMatrix m = newIDWTMatrix(n);
		final BasicMatrix g = m.solve(f);
		
		debugPrint(m);
		debugPrint(f);
		debugPrint(g);
		debugPrint(g.transpose().multiplyRight(newWaveColumn(n, 0.0 / n)).doubleValue(0));
		debugPrint(g.transpose().multiplyRight(newWaveColumn(n, 1.0 / n)).doubleValue(0));
		debugPrint(g.transpose().multiplyRight(newWaveColumn(n, 2.0 / n)).doubleValue(0));
	}
	
	public static final void demo2D() {
		final int n1 = 5;
		final double[] values = Arrays.stream(intRange(n1 * n1)).mapToDouble(x -> x).toArray();
		values[n1 * n1 / 2] *= -1.0;
		final BasicMatrix f = newColumn(values);
		final int n2 = f.size() / n1;
		
		debugPrint(n1, n2);
		
		final BasicMatrix m = newIDWTMatrix(n1, n2);
		
		if (f.size() < 50) {
			debugPrint(m);
		}
		
		final int rank = m.getRank();
		
		if (rank != f.size()) {
			debugError(rank);
		}
		
		final BasicMatrix g = m.solve(f);
		
		debugPrint(f);
		debugPrint(g);
		
		for (int x1 = 0; x1 < n1; ++x1) {
			for (int x2 = 0; x2 < n2; ++x2) {
				debugPrint(x1, x2, g.transpose().multiplyRight(newWaveColumn(n1, n2, (double) x1 / n1, (double) x2 / n2)).doubleValue(0));
			}
		}
	}
	
	public static final void demoND() {
		final int[] ns = { 3, 3, 3 };
		final double[] values = Arrays.stream(intRange(product(ns))).mapToDouble(x -> x).toArray();
		values[values.length / 2] *= -1.0;
		final BasicMatrix f = newColumn(values);
		
		debugPrint(Arrays.toString(ns));
		
		final BasicMatrix m = newIDWTMatrix(ns);
		
		if (f.size() < 50) {
			debugPrint(m);
		}
		
		final int rank = m.getRank();
		
		if (rank != f.size()) {
			debugError(rank);
		}
		
		final BasicMatrix g = m.solve(f);
		
		debugPrint(f);
		debugPrint(g);
		
		{
			final double[] normalizedXs = new double[ns.length];
			
			for (final int[] xs : cartesian(bounds(ns))) {
				normalize(xs, ns, normalizedXs);
				
				debugPrint(Arrays.toString(xs), g.transpose().multiplyRight(newWaveColumn(ns, normalizedXs)).doubleValue(0));
			}
		}
	}
	
	public static final BasicMatrix newColumn(final double... values) {
		final int n = values.length;
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int i = 0; i < n; ++i) {
			builder.set(i, values[i]);
		}
		
		return builder.build();
	}
	
	public static final BasicMatrix newIDWTMatrix(final int n) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, n);
		
		for (int x = 0; x < n; ++x) {
			for (int k = 0; k < n; ++k) {
				builder.set(x, k, waveCell((double) x / n, k));
			}
		}
		
		return builder.build();
	}
	
	public static final double waveCell(final double x, final int k) {
		return wave(1 + k, k * x / (1 + k));
	}
	
	public static final BasicMatrix newWaveColumn(final int n, final double x) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int k = 0; k < n; ++k) {
			builder.set(k, waveCell(x, k));
		}
		
		return builder.build();
	}
	
	public static final BasicMatrix newIDWTMatrix(final int[] ns) {
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
				
				builder.set(i, j, waveCell(normalizedXs, ks));
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
	
	public static final int sum(final int... values) {
		return Arrays.stream(values).sum();
	}
	
	public static final int product(final int... values) {
		return Arrays.stream(values).reduce(1, (x, y) -> x * y);
	}
	
	public static final double waveCell(final double[] xs, final int[] ks) {
		final int m = xs.length;
		final int n = 1 + sum(ks);
		final int count = 1 << (m - 1);
		double result = 0.0;
		
		for (int i = 0; i < count; ++i) {
			double argument = ks[0] * xs[0] / n;
			
			for (int j = 1; j < m; ++j) {
				final int signum = ((i >> (j - 1)) & 1) * 2 - 1;
				argument += signum * ks[j] * xs[j] / n;
			}
			
			result += wave(n, argument);
		}
		
		return result;
	}
	
	public static final BasicMatrix newWaveColumn(final int[] ns, final double[] xs) {
		final int n = product(ns);
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		int i = -1;
		
		for (final int[] ks : cartesian(bounds(ns))) {
			builder.set(++i, 0, waveCell(xs, ks));
		}
		
		return builder.build();
		
	}
	
	public static final BasicMatrix newIDWTMatrix(final int n1, final int n2) {
		final int n = n1 * n2;
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, n);
		
		for (int x1 = 0, i = 0; x1 < n1; ++x1) {
			for (int x2 = 0; x2 < n2; ++x2, ++i) {
				for (int k1 = 0, j = 0; k1 < n1; ++k1) {
					for (int k2 = 0; k2 < n2; ++k2, ++j) {
						builder.set(i, j, waveCell((double) x1 / n1, k1, (double) x2 / n2, k2));
					}
				}
			}
		}
		
		return builder.build();
	}
	
	public static final double waveCell(final double x1, final int k1, final double x2, final int k2) {
		final int n = 1 + k1 + k2;
		final double xx1 = k1 * x1 / n;
		final double xx2 = k2 * x2 / n;
		
		return wave(n, xx1 + xx2) + wave(n, xx1 - xx2);
	}
	
	public static final BasicMatrix newWaveColumn(final int n1, final int n2, final double x1, final double x2) {
		final int n = n1 * n2;
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int k1 = 0, i = 0; k1 < n1; ++k1) {
			for (int k2 = 0; k2 < n2; ++k2, ++i) {
				builder.set(i, waveCell(x1, k1, x2, k2));
			}
		}
		
		return builder.build();
	}
	
	public static final double wave(final int n, final double x) {
		double result = 1.0 - relu(n * x);
		
		for (int k = 1, s = 2; k < n; ++k, s = -s) {
			result += s * relu(n * x - k);
		}
		
		return result;
	}
	
	public static final double relu(final double x) {
		return max(0.0, x);
	}
	
}
