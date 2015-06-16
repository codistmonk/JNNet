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
	}
	
	public static final void demo1D() {
		final BasicMatrix f = newColumn(1.0, 2.0, 3.0);
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
	
	public static final BasicMatrix newWaveColumn(final int n, final double x) {
		final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n, 1);
		
		for (int i = 0; i < n; ++i) {
			builder.set(i, wave(i, x));
		}
		
		return builder.build();
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
				builder.set(x, k, wave(k, (double) x / n));
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
