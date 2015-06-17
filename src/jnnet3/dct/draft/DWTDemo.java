package jnnet3.dct.draft;

import static java.lang.Math.*;
import static multij.tools.Tools.*;

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
		final BasicMatrix f = newColumn(
				1.0, 2.0, 3.0,
				4.0, -5.0, 6.0,
				7.0, 8.0, 9.0/*, 10.0, 11.0, 12.0,
				13.0, 14.0, 15.0, 16.0*/);
		final int n1 = 3;
		final int n2 = f.size() / n1;
		final BasicMatrix m = newIDWTMatrix(n1, n2);
		
		debugPrint(m);
		
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
				builder.set(x, k, wave(k + 1, (double) x / n));
			}
		}
		
		return builder.build();
	}
	
	public static final BasicMatrix newWaveColumn(final int n, final double x) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int i = 0; i < n; ++i) {
			builder.set(i, wave(i + 1, x));
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
//		return wave(1 + k1, x1) + wave(1 + k2, x2);
		return wave(1 + k1 + k2, k1 * x1 + k2 * x2);
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
