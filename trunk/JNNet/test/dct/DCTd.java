package dct;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static dct.MiniCAS.*;

import dct.MiniCAS.Expression;

import java.util.Arrays;
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
		{
			final Expression[] f = expressions(121, 58);
			final Expression[] dct = constants(dct(f, 0), dct(f, 1));
			final Expression[] g = constants(idct(dct, 0), idct(dct, 1));
			
			Tools.debugPrint(dct);
			Tools.debugPrint(g);
		}
		
		{
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
			
			Tools.debugPrint(Arrays.deepToString(dct));
			Tools.debugPrint(Arrays.deepToString(g));
		}
	}
	
	public static final Expression dct(final Object f, final Object... indices) {
		return apply(DCTd::dct, f, indices, 0);
	}
	
	public static final Expression idct(final Object f, final Object... indices) {
		return apply(DCTd::idct, f, indices, 0);
	}
	
	public static final Expression apply(final BiFunction<Expression[], Object, Expression> transform, final Object f, final Object... indices) {
		return apply(transform, f, indices, 0);
	}
	
	private static final Expression apply(final BiFunction<Expression[], Object, Expression> transform, final Object f, final Object[] indices, final int indexIndex) {
		final Expression[] values;
		
		if (Expression.class.isAssignableFrom(f.getClass().getComponentType())) {
			values = (Expression[]) f;
		} else {
			values = Arrays.stream((Object[]) f).map(v -> apply(transform, v, indices, indexIndex + 1)).toArray(Expression[]::new);
		}
		
		return dct(values, indices[indexIndex]);
	}
	
	public static final Expression dct(final Expression[] f, final Object k) {
		final int n = f.length;
		final Expression expressionN = expression(n);
		final Expression expressionK = expression(k);
		Expression result = constant(0.0);
		
		if (ZERO.equals(expressionK)) {
			for (int x = 0; x < n; ++x) {
				result = add(result, f[x]);
			}
		} else {
			for (int x = 0; x < n; ++x) {
				result = add(result, dct1k(f[x], expression(x), expressionK, expressionN));
			}
			
			result = multiply(result, sqrt(2.0));
		}
		
		return divide(result, sqrt(n));
	}
	
	public static final Expression idct(final Expression[] dct, final Object x) {
		final int n = dct.length;
		Expression result = dct[0];
		
		for (int k = 1; k < n; ++k) {
			result = add(result, idct1k(dct[k], expression(x), expression(k), expression(n)));
		}
		
		return divide(result, sqrt(n));
	}
	
	public static final Expression dct1k(final Expression a, final Expression x, final Expression k, final Expression n) {
		return multiply(a, cos(multiply(add(multiply(2.0, x), 1.0), k, PI, invert(2.0), invert(n))));
	}
	
	public static final Expression idct1k(final Expression a, final Expression x, final Expression k, final Expression n) {
		return multiply(dct1k(a, x, k, n), sqrt(2.0));
	}
	
}
