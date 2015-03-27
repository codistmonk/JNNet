package dct;

import static imj3.tools.CommonTools.cartesian;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.cast;
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
public final class DCT {
	
	private DCT() {
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
			final int n = f.length;
			final double[][] dct = new double[n][n];
			
//			dct1(f, dct);
//			dct1(transpose(dct), dct);
			
			dct(f, 0, dct);
			dct(dct, 1, dct);
			
			final double[][] y = new double[n][n];
			
			idct1(dct, y);
			idct1(transpose(y), y);
			
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
		final int n = Array.getLength(f);
		
		for (int i = 0; i < n; ++i) {
			dct(result, i, result);
		}
		
		return result;
	}
	
	public static final <T> T idct(final T f) {
		final T result = deepClone(f);
		final int n = Array.getLength(f);
		
		for (int i = 0; i < n; ++i) {
			idct(result, i, result);
		}
		
		return result;
	}
	
	public static final <T> T dct(final T f, final int dimensionIndex, final T result) {
		return transform(f, dimensionIndex, result, DCT::dct);
	}
	
	public static final <T> T idct(final T f, final int dimensionIndex, final T result) {
		return transform(f, dimensionIndex, result, DCT::idct);
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
	
	public static final double get(final Object array, final int... indices) {
		final int n = indices.length;
		int i;
		Object row = array;
		
		for (i = 0; i + 1 < n; ++i) {
			row = Array.get(row, indices[i]);
		}
		
		return Array.getDouble(row, indices[i]);
	}
	
	public static final void put(final double value, final Object array, final int... indices) {
		final int n = indices.length;
		int i;
		Object row = array;
		
		for (i = 0; i + 1 < n; ++i) {
			row = Array.get(row, indices[i]);
		}
		
		Array.setDouble(row, indices[i], value);
	}
	
	public static final double[] dct(final double[] f, final double[] result) {
		Arrays.fill(result, 0.0);
		
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
		
		for (int x = 0; x < n; ++x) {
			result += dct1(f[x], x, k, n);
		}
		
		return result;
	}
	
	public static final double idct(final double[] dct, final int x) {
		final int n = dct.length;
		double result = 0.0;
		
		for (int k = 0; k < n; ++k) {
			result += dct1(dct[k], x, k, n);
		}
		
		return result;
	}
	
	public static final double dct1(final double a, final int x, int k, final int n) {
		return a * cos((2.0 * x + 1.0) * k * PI / (2.0 * n)) * c(k, n);
	}
	
	public static final double c(final int k, final double n) {
		return k == 0 ? 1.0 / sqrt(n) : sqrt(2.0) / sqrt(n);
	}
	
	public static final  <T> T mapDCT(final T f) {
		final Object[] array = (Object[]) f;
		final int n = array.length;
		Object[] result = array.clone();
		
		for (int i = 0; i < n; ++i) {
			final double[] values = cast(double[].class, array[i]);
			
			if (values != null) {
				result[i] = dct(values, new double[values.length]);
			} else {
				result[i] = mapDCT((Object[]) array[i]);
			}
		}
		
		return (T) result;
	}
	
	public static final void dct1(final double[][] x, final double[][] result) {
		final int n = x.length;
		
		for (int i = 0; i < n; ++i) {
			dct(x[i], result[i]);
		}
	}
	
	public static final void idct1(final double[][] dct, final double[][] result) {
		final int n = dct.length;
		
		for (int i = 0; i < n; ++i) {
			idct(dct[i], result[i]);
		}
	}
	
	public static final double[][] transpose(final double[][] matrix) {
		final int n1 = matrix.length;
		final int n2 = matrix[0].length;
		final double[][] result = new double[n2][n1];
		
		for (int i = 0; i < n1; ++i) {
			for (int j = 0; j < n2; ++j) {
				result[j][i] = matrix[i][j];
			}
		}
		
		return result;
	}
	
}
