package dct;

import static dct.DCTa.get;
import static dct.DCTa.getDimensionCount;
import static dct.DCTa.put;
import static imj3.tools.CommonTools.cartesian;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.deepClone;
import static net.sourceforge.aprog.tools.Tools.swap;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-23)
 */
public final class DCTb {
	
	private DCTb() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		{
//			final double[] f = { 1.0, 2.0, 3.0, 4.0 };
//			final double[] f = { 121, 58, 61, 113, 171, 200, 226, 246 };
//			final double[] f = { 121, 58 };
			final double[] f = { 121, 58, 61, 113 };
			final int n = f.length;
			final double[] dct = new double[n];
			
			dct(f, dct);
			
			final double[] y = new double[n];
			
			idct(dct, y);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.toString(f));
			Tools.debugPrint(Arrays.toString(dct));
			Tools.debugPrint(Arrays.toString(y));
		}
		
		{
			final double[][] f = {
					{ 1.0, 2.0, 3.0, 4.0 },
					{ 2.0, 3.0, 4.0, 1.0 },
					{ 3.0, 4.0, 1.0, 2.0 },
					{ 4.0, 1.0, 2.0, 3.0 },
			};
			final double[][] dct = dct(f);
			final double[][] y = idct(dct);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.deepToString(dct));
			Tools.debugPrint(Arrays.deepToString(y));
		}
		
		{
			final double[][][] f = {
					{
						{ 1.0, 2.0 },
						{ 3.0, 6.0 },
					},
					{
						{ 5.0, 4.0 },
						{ 7.0, 8.0 },
					},
			};
			final double[][][] dct = dct(f);
			final double[][][] y = idct(dct);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.deepToString(dct));
			Tools.debugPrint(Arrays.deepToString(y));
		}
	}
	
	public static final <T> T dct(final T f) {
		final T result = deepClone(f);
		final int n = getDimensionCount(f.getClass());
		
		for (int i = 0; i < n; ++i) {
			dct(result, i, result);
		}
		
		return result;
	}
	
	public static final <T> T idct(final T dct) {
		final T result = deepClone(dct);
		final int n = getDimensionCount(dct.getClass());
		
		for (int i = 0; i < n; ++i) {
			idct(result, i, result);
		}
		
		return result;
	}
	
	public static final <T> T dct(final T f, final int dimensionIndex, final T result) {
		return transform(f, dimensionIndex, result, DCTb::dct);
	}
	
	public static final <T> T idct(final T f, final int dimensionIndex, final T result) {
		return transform(f, dimensionIndex, result, DCTb::idct);
	}
	
	public static final <T> T transform(final T f, final int dimensionIndex, final T result, final Transformation transformation) {
		final List<Integer> dimensions = new ArrayList<>();
		
		{
			Object tmp = f;
			
			while (tmp.getClass().isArray()) {
				dimensions.add(Array.getLength(tmp));
				tmp = Array.get(tmp, 0);
			}
		}
		
		final int n = dimensions.size();
		final int[] minMaxes = new int[n * 2];
		
		for (int i = 0; i < n; ++i) {
			minMaxes[2 * i + 1] = dimensions.get(i) - 1;
		}
		
		final double[] inputBuffer = new double[dimensions.get(dimensionIndex)];
		final double[] outputBuffer = inputBuffer.clone();
		
		swap(minMaxes, 2 * dimensionIndex + 1, 2 * n - 1);
		
		final Iterator<int[]> resultIndicesIterator = cartesian(minMaxes).iterator();
		int i = 0;
		
		for (final int[] indices : cartesian(minMaxes)) {
			swap(indices, dimensionIndex, n - 1);
			inputBuffer[i] = get(f, indices);
			swap(indices, dimensionIndex, n - 1);
			
			if (++i == inputBuffer.length) {
				i = 0;
				
				transformation.transform(inputBuffer, outputBuffer);
				
				for (final double value : outputBuffer) {
					if (resultIndicesIterator.hasNext()) {
						final int[] outputIndices = resultIndicesIterator.next();
						swap(outputIndices, dimensionIndex, n - 1);
						put(value, result, outputIndices);
						swap(outputIndices, dimensionIndex, n - 1);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-25)
	 */
	public static abstract interface Transformation extends Serializable {
		
		public abstract void transform(double[] input, double[] output);
		
	}
	
	public static final double[] dct(final double[] f, final double[] result) {
		final int n = f.length;
		
		for (int k = 0; k < n; ++k) {
			result[k] = dct(f, k);
		}
		
		return result;
	}
	
	public static final double[] idct(final double[] dct, final double[] result) {
		final int n = dct.length;
		
		for (int x = 0; x < n; ++x) {
			result[x] = idct(dct, x);
		}
		
		return result;
	}
	
	public static final double dct(final double[] f, final int k) {
		final int n = f.length;
		double result = 0.0;
		
		if (k == 0) {
			for (int x = 0; x < n; ++x) {
				result += f[x];
			}
		} else {
			for (int x = 0; x < n; ++x) {
				result += dct1k(f[x], x, k, n);
			}
			
			result *= sqrt(2.0);
		}
		
		return result / sqrt(n);
	}
	
	public static final double idct(final double[] dct, final int x) {
		final int n = dct.length;
		double result = dct[0];
		
		for (int k = 1; k < n; ++k) {
			result += idct1k(dct[k], x, k, n);
		}
		
		return result / sqrt(n);
	}
	
	public static final double dct1k(final double a, final int x, final int k, final int n) {
		return a * cos((2.0 * x + 1.0) * k * PI / (2.0 * n));
	}
	
	public static final double idct1k(final double a, final int x, final int k, final int n) {
		return dct1k(a, x, k, n) * sqrt(2.0);
	}
	
}
