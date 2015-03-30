package dct;

import static java.lang.Math.PI;
import static java.util.stream.Collectors.toList;
import static net.sourceforge.aprog.tools.Tools.cast;
import static dct.MiniCAS.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-29)
 */
public final class DCTd {
	
	private DCTd() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		if (true) {
			final Expression[] f = expressions(121, 58);
			final Expression[] dct = constants(dct(f, 0), dct(f, 1));
			final Expression[] g = constants(idct(dct, 0), idct(dct, 1));
			
			Tools.debugPrint();
			Tools.debugPrint((Object[]) f);
			Tools.debugPrint((Object[]) dct);
			Tools.debugPrint((Object[]) g);
		}
		
		if (true) {
			final Expression[][] f = {
					expressions(1, 2),
					expressions(2, 3),
			};
			final Expression[][] dct = {
					constants(dct(f, 0, 0), dct(f, 0, 1)),
					constants(dct(f, 1, 0), dct(f, 1, 1)),
			};
			final Expression[][] g = {
					constants(idct(dct, 0, 0), idct(dct, 0, 1)),
					constants(idct(dct, 1, 0), idct(dct, 1, 1)),
			};
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.deepToString(dct));
			Tools.debugPrint(Arrays.deepToString(g));
		}
		
		if (true) {
			final Expression[][][] f = {
					{
						constants(1.0, 2.0),
						constants(3.0, 6.0),
					},
					{
						constants(5.0, 4.0),
						constants(7.0, 8.0),
					},
			};
			final Expression[][][] dct = {
					{
						constants(dct(f, 0, 0, 0), dct(f, 0, 0, 1)),
						constants(dct(f, 0, 1, 0), dct(f, 0, 1, 1)),
					},
					{
						constants(dct(f, 1, 0, 0), dct(f, 1, 0, 1)),
						constants(dct(f, 1, 1, 0), dct(f, 1, 1, 1)),
					},
			};
			final Expression[][][] g = {
					{
						constants(idct(dct, 0, 0, 0), idct(dct, 0, 0, 1)),
						constants(idct(dct, 0, 1, 0), idct(dct, 0, 1, 1)),
					},
					{
						constants(idct(dct, 1, 0, 0), idct(dct, 1, 0, 1)),
						constants(idct(dct, 1, 1, 0), idct(dct, 1, 1, 1)),
					},
			};
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.deepToString(dct));
			Tools.debugPrint(Arrays.deepToString(g));
			
			final Variable x1 = variable("x1");
			final Variable x2 = variable("x2");
			final Variable x3 = variable("x3");
			final Expression expression = approximate(separateCosProducts(idct(dct, x1, x2, x3)), 1.0E-8);
			
			x1.setValue(expression(0));
			x2.setValue(expression(1));
			x3.setValue(expression(1));
			
			Tools.debugPrint(expression.getAsDouble());
			
			if (true) {
				Tools.debugPrint();
				((Sum) expression).getOperands().forEach(System.out::println);
			}
		}
	}
	
	public static final Expression approximate(final Expression expression, final double epsilon) {
		return limitOf(expression, Canonicalize.INSTANCE, new Approximate(epsilon));
	}
	
	public static final Expression separateCosProducts(final Expression expression) {
		return limitOf(expression, Canonicalize.INSTANCE, CosProductSeparator.INSTANCE);
	}
	
	public static final Expression dct(final Object f, final Object... indices) {
		return apply(DCTd::dct, f, indices, 0);
	}
	
	public static final Expression idct(final Object f, final Object... indices) {
		return apply(DCTd::idct, f, indices, 0);
	}
	
	public static final Expression apply(final BiFunction<Expression[], Object, Expression> transform,
			final Object f, final Object... indices) {
		return apply(transform, f, indices, 0);
	}
	
	private static final Expression apply(final BiFunction<Expression[], Object, Expression> transform,
			final Object f, final Object[] indices, final int indexIndex) {
		final Expression[] values;
		
		if (Expression.class.isAssignableFrom(f.getClass().getComponentType())) {
			values = (Expression[]) f;
		} else {
			values = Arrays.stream((Object[]) f).map(v -> apply(transform, v, indices, indexIndex + 1)).toArray(Expression[]::new);
		}
		
		return transform.apply(values, indices[indexIndex]);
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
				result = add(result, dct1(f[x], expression(x), expressionK, expressionN));
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
			result = add(result, dct1(dct[k], expressionX, expression(k), expressionN));
		}
		
		return result;
	}
	
	public static final Expression dct1(final Expression a, final Expression x, final Expression k, final Expression n) {
		final Expression cos = cos(multiply(add(multiply(2.0, x), 1.0), k, PI, invert(2.0), invert(n)));
		return multiply(multiply(a, cos), sqrt(2.0), invert(sqrt(n)));
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
