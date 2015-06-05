package jnnet3.dct;

import static imj3.tools.CommonTools.cartesian;
import static java.lang.Math.PI;
import static java.util.stream.Collectors.toCollection;
import static jnnet3.dct.MiniCAS.*;
import static multij.tools.Tools.*;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import multij.tools.IllegalInstantiationException;

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
	
	public static final Expression separateCosProducts(final Expression expression, final double epsilon) {
		return limitOf(approximate(expression, epsilon), CosProductSeparator.INSTANCE,
				Canonicalize.INSTANCE, new Approximate(epsilon)).accept(new AddPhasors(epsilon));
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
		final int[] dimensions = getDimensions(f);
		final int n = dimensions.length;
		final int[] minMaxes = new int[n * 2];
		
		for (int i = 0; i < n; ++i) {
			minMaxes[2 * i + 1] = dimensions[i] - 1;
		}
		
		final Expression[] inputBuffer = new Expression[dimensions[dimensionIndex]];
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
	
	public static final <T> int[] getDimensions(final T array) {
		final int n = getDimensionCount(array.getClass());
		final int[] result = new int[n];
		Object tmp = array;
		
		for (int i = 0; i < n && tmp != null; ++i) {
			final int m = Array.getLength(tmp);
			
			if (0 < m) {
				result[i] = m;
				tmp = Array.get(tmp, 0);
			} else {
				tmp = null;
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
			final List<Expression> operands = operation.getOperands().stream().map(this).collect(toCollection(ArrayList::new));
			
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
	
	/**
	 * @author codistmonk (creation 2015-04-02)
	 */
	public static final class AddPhasors implements Expression.Rewriter {
		
		private final double epsilon;
		
		public AddPhasors(final double epsilon) {
			this.epsilon = epsilon;
		}
		
		@Override
		public final Expression visit(final NaryOperation operation) {
			final List<Expression> operands = operation.getOperands().stream().map(this).collect(toCollection(ArrayList::new));
			final Sum sum = cast(Sum.class, operation);
			
			if (sum != null) {
				try {
					final List<CosTerm> cosTerms = operands.stream().map(CosTerm::new).collect(toCollection(ArrayList::new));
					
					operands.clear();
					
					for (int i = 0; i < cosTerms.size(); ++i) {
						final CosTerm cosTermI = cosTerms.get(i);
						final Expression approximatedFrequencyI = approximate(cosTermI.getFrequency(), this.epsilon);
						final List<Double> magnitudes = new ArrayList<>();
						final List<Double> phases = new ArrayList<>();
						
						magnitudes.add(cosTermI.getMagnitude().getAsDouble());
						phases.add(cosTermI.getPhase().getAsDouble());
						
						for (int j = i + 1; j < cosTerms.size(); ++j) {
							final CosTerm cosTermJ = cosTerms.get(j);
							final Expression approximatedFrequencyJ = approximate(cosTermJ.getFrequency(), this.epsilon);
							final Expression approximatedOppositeFrequencyJ = approximate(
									multiply(MINUS_ONE, cosTermJ.getFrequency()), this.epsilon);
							
							if (approximatedFrequencyI.equals(approximatedFrequencyJ, this.epsilon)) {
								phases.add(cosTermJ.getPhase().getAsDouble());
							} else if (approximatedFrequencyI.equals(approximatedOppositeFrequencyJ, this.epsilon)) {
								phases.add(-cosTermJ.getPhase().getAsDouble());
							} else {
								continue;
							}
							
							magnitudes.add(cosTermJ.getMagnitude().getAsDouble());
							cosTerms.remove(j);
							--j;
						}
						
						final int n = magnitudes.size();
						
						if (n == 1) {
							operands.add(cosTermI.getTerm());
						} else {
							double a2 = 0.0;
							double tandN = 0.0;
							double tandD = 0.0;
							
							for (int j = 0; j < n; ++j) {
								final double mj = magnitudes.get(j);
								final double pj = phases.get(j);
								
								for (int k = 0; k < n; ++k) {
									final double mk = magnitudes.get(k);
									final double pk = phases.get(k);
									
									a2 += mj * mk * Math.cos(pj - pk);
								}
								
								tandN += mj * Math.sin(pj);
								tandD += mj * Math.cos(pj);
							}
							
							final double magnitude = Math.sqrt(Math.max(this.epsilon, a2));
							final double phase = Math.atan2(tandN, tandD);
							
							operands.add(multiply(magnitude, cos(add(phase, cosTermI.getFrequency()))));
						}
					}
				} catch (final Exception exception) {
					ignore(exception);
				}
			}
			
			// XXX find out how to really adjust epsilon to minimize error
			return approximate(operation.maybeNew(operands), Math.sqrt(this.epsilon) * 2.0);
		}
		
		private static final long serialVersionUID = -2969190749620211785L;
		
		/**
		 * @author codistmonk (creation 2015-04-02)
		 */
		public static final class CosTerm implements Serializable {
			
			private final Expression term;
			
			private final Constant magnitude;
			
			private final Cos cos;
			
			private final Constant phase;
			
			private final Expression frequency;
			
			public CosTerm(final Expression term) {
				this.term = term;
				
				{
					final Product product = cast(Product.class, term);
					
					if (product != null) {
						if (product.getOperands().size() != 2) {
							throw new IllegalArgumentException(term.toString());
						}
						
						this.magnitude = (Constant) product.getOperands().get(0);
						this.cos = (Cos) product.getOperands().get(1);
					} else {
						final Cos cos = cast(Cos.class, term);
						
						if (cos != null) {
							this.magnitude = ONE;
							this.cos = cos;
						} else {
							this.magnitude = (Constant) term;
							this.cos = cos(ZERO);
						}
					}
				}
				
				{
					final Expression argument = this.cos.getOperand();
					final Sum sum = cast(Sum.class, argument);
					
					if (sum != null) {
						final List<Expression> sumOperands = sum.getOperands();
						final Constant phase = cast(Constant.class, sumOperands.get(0));
						
						if (phase != null) {
							this.phase = phase;
							this.frequency = add(sumOperands.subList(1, sumOperands.size()).toArray());
						} else {
							this.phase = ZERO;
							this.frequency = argument;
						}
					} else {
						final Constant phase = cast(Constant.class, argument);
						
						if (phase != null) {
							this.phase = phase;
							this.frequency = ZERO;
						} else {
							this.phase = ZERO;
							this.frequency = argument;
						}
					}
				}
			}
			
			public final Expression getTerm() {
				return this.term;
			}
			
			public final Constant getMagnitude() {
				return this.magnitude;
			}
			
			public final Cos getCos() {
				return this.cos;
			}
			
			public final Constant getPhase() {
				return this.phase;
			}
			
			public final Expression getFrequency() {
				return this.frequency;
			}
			
			@Override
			public final String toString() {
				return "(" + this.getMagnitude() + " " + this.getPhase() + " " + this.getFrequency() + ")";
			}
			
			private static final long serialVersionUID = -4928748997284860125L;
			
		}
		
	}
	
}
