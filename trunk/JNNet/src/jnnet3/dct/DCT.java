package jnnet3.dct;

import static imj3.tools.CommonTools.cartesian;
import static java.lang.Math.PI;
import static java.util.stream.Collectors.toList;
import static jnnet3.dct.MiniCAS.*;
import static net.sourceforge.aprog.tools.Tools.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-03-29)
 */
public final class DCT {
	
	private DCT() {
		throw new IllegalInstantiationException();
	}
	
	public static final <T, I, O> T deepApply(final Function<I, O> transform, final T f) {
		return deepApply(transform, f, f);
	}
	
	public static final <T, I, O> T deepApply(final Function<I, O> transform, final T f, final T result) {
		final T actualResult = result == null ? deepClone(f) : result;
		final int n = Array.getLength(f);
		
		if (Expression.class.isAssignableFrom(f.getClass().getComponentType())) {
			for (int i = 0; i < n; ++i) {
				Array.set(actualResult, i, transform.apply((I) Array.get(f, i)));
			}
		} else {
			for (int i = 0; i < n; ++i) {
				deepApply(transform, Array.get(f, i), Array.get(actualResult, i));
			}
		}
		
		return actualResult;
	}
	
	public static final double[] toDoubles(final Expression... expressions) {
		return Arrays.stream(expressions).mapToDouble(Expression::getAsDouble).toArray();
	}
	
	@SuppressWarnings("unchecked")
	public static final <T, U> T deepToDoubles(final U array) {
		final Class<? extends Object> cls = array.getClass();
		final int n = Array.getLength(array);
		
		if (cls.getComponentType().isArray()) {
			final Class<?> deepDoublesClass = getDeepDoublesClass(getDimensionCount(cls.getComponentType()));
			final Object result = Array.newInstance(deepDoublesClass, n);
			
			for (int i = 0; i < n; ++i) {
				Array.set(result, i, deepToDoubles(Array.get(array, i)));
			}
			
			return (T) result;
		}
		
		return (T) toDoubles((Expression[]) array);
	}
	
	public static final int getDimensionCount(final Class<?> cls) {
		if (!cls.isArray()) {
			return 0;
		}
		
		return 1 + getDimensionCount(cls.getComponentType());
	}
	
	private static final Class<?> getDeepDoublesClass(final int n) {
		Class<?> result = double[].class;
		
		for (int i = 1; i < n; ++i) {
			result = Array.newInstance(result, 0).getClass();
		}
		
		return result;
	}
	
	public static final Expression separateCosProducts(final Expression expression) {
		return limitOf(expression, Canonicalize.INSTANCE, CosProductSeparator.INSTANCE);
	}
	
	public static final Expression dct(final Object f, final Object... indices) {
		return apply(DCT::dct, f, indices, 0);
	}
	
	public static final Expression idct(final Object f, final Object... indices) {
		return apply(DCT::idct, f, indices, 0);
	}
	
	public static final Expression apply(final BiFunction<Expression[], Object, Expression> transform,
			final Object f, final Object... indices) {
		return apply(transform, f, indices, 0);
	}
	
	public static final Expression apply(final BiFunction<Expression[], Object, Expression> transform,
			final Object f, final Object[] indices, final int indexIndex) {
		final Expression[] values;
		
		if (Expression.class.isAssignableFrom(f.getClass().getComponentType())) {
			values = (Expression[]) f;
		} else {
			values = Arrays.stream((Object[]) f).map(v -> apply(transform, v, indices, indexIndex + 1)).toArray(Expression[]::new);
		}
		
		return transform.apply(values, indices[indexIndex]);
	}
	
	public static final <T> Expression[] apply(final BiFunction<Expression[], Object, Expression> transform,
			final Expression[] f, final Expression[] result) {
		final int n = result.length;
		
		for (int i = 0; i < n; ++i) {
			result[i] = transform.apply(f, i);
		}
		
		return result;
	}
	
	public static final <T> T fullDCT(final T f) {
		return applyToAllDimensions(DCT::dct, f, deepClone(f));
	}
	
	public static final <T> T fullIDCT(final T f) {
		return applyToAllDimensions(DCT::idct, f, deepClone(f));
	}
	
	public static final <T> T applyToAllDimensions(final BiFunction<Expression[], Object, Expression> transform, final T f, final T result) {
		final int n = getDimensionCount(f.getClass());
		
		for (int i = 0; i < n; ++i) {
			applyToDimension(i, transform, i == 0 ? f : result, result);
		}
		
		return result;
	}
	
	public static final <T> T applyToDimension(final int dimensionIndex, final BiFunction<Expression[], Object, Expression> transform,
			final T f, final T result) {
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
		
		final Expression[] inputBuffer = new Expression[dimensions.get(dimensionIndex)];
		final Expression[] outputBuffer = inputBuffer.clone();
		
		swap(minMaxes, 2 * dimensionIndex + 1, 2 * n - 1);
		
		final Iterator<int[]> resultIndicesIterator = cartesian(minMaxes).iterator();
		int i = 0;
		
		for (final int[] indices : cartesian(minMaxes)) {
			
			swap(indices, dimensionIndex, n - 1);
			inputBuffer[i] = get(f, indices);
			swap(indices, dimensionIndex, n - 1);
			
			if (++i == inputBuffer.length) {
				i = 0;
				
				apply(transform, inputBuffer, outputBuffer);
				
				for (final Expression value : outputBuffer) {
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
	
	@SuppressWarnings("unchecked")
	public static final <T> T get(final Object array, final int... indices) {
		final int n = indices.length;
		int i;
		Object row = array;
		
		for (i = 0; i + 1 < n; ++i) {
			row = Array.get(row, indices[i]);
		}
		
		return (T) Array.get(row, indices[i]);
	}
	
	public static final void put(final Object value, final Object array, final int... indices) {
		final int n = indices.length;
		int i;
		Object row = array;
		
		for (i = 0; i + 1 < n; ++i) {
			row = Array.get(row, indices[i]);
		}
		
		Array.set(row, indices[i], value);
	}
	
	public static final Expression dct(final Expression[] f, final Object k) {
		final int n = f.length;
		final Expression expressionN = expression(n);
		final Expression expressionK = expression(k);
		final Expression sqrtN = sqrt(expressionN);
		Expression result = constant(0.0);
		
		if (ZERO.equals(expressionK)) {
			for (int x = 0; x < n; ++x) {
				result = add(result, divide(f[x], sqrtN));
			}
		} else {
			for (int x = 0; x < n; ++x) {
				result = add(result, cosTerm(f[x], expression(x), expressionK, expressionN));
			}
		}
		
		return result;
	}
	
	public static final Expression idct(final Expression[] dct, final Object x) {
		final int n = dct.length;
		final Expression expressionN = expression(n);
		final Expression expressionX = expression(x);
		final Expression sqrtN = sqrt(expressionN);
		Expression result = divide(dct[0], sqrtN);
		
		for (int k = 1; k < n; ++k) {
			result = add(result, cosTerm(dct[k], expressionX, expression(k), expressionN));
		}
		
		return result;
	}
	
	public static final Expression cosTerm(final Expression a, final Expression x, final Expression k, final Expression n) {
		final Expression cos = cos(multiply(add(multiply(2.0, x), 1.0), k, PI, invert(2.0), invert(n)));
		return multiply(a, cos, sqrt(2.0), invert(sqrt(n)));
	}
	
	/**
	 * @author codistmonk (creation 2015-03-30)
	 */
	public static final class CosProductSeparator implements Expression.Rewriter {
		
		@Override
		public final Expression visit(final NaryOperation operation) {
			final List<Expression> operands = operation.getOperands().stream().map(this).collect(toList());
			
			for (int i = 0, n = operands.size(); i < n; ++i) {
				final Cos cosI = cast(Cos.class, operands.get(i));
				
				if (cosI != null) {
					final Expression u = cosI.getOperand();
					
					for (int j = i + 1; j < n; ++j) {
						final Cos cosJ = cast(Cos.class, operands.get(j));
						
						if (cosJ != null) {
							final Expression v = cosJ.getOperand();
							
							operands.set(i, multiply(0.5, add(cos(subtract(u, v)), cos(add(u, v)))));
							operands.remove(j);
							--n;
							break;
						}
					}
				}
				
			}
			
			return operation.maybeNew(operands);
		}
		
		private static final long serialVersionUID = -4592831680416231630L;
		
		public static final CosProductSeparator INSTANCE = new CosProductSeparator();
		
	}
	
}
