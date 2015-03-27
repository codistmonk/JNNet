package dct;

import static dct.DCTa.getDimensionCount;
import static imj3.tools.CommonTools.cartesian;
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.deepClone;
import static net.sourceforge.aprog.tools.Tools.swap;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleSupplier;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-23)
 */
public final class DCTc {
	
	private DCTc() {
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
			final Expression[] f = constants(121, 58, 61, 113);
			final int n = f.length;
			final Expression[] dct = new Expression[n];
			
			dct(f, dct);
			
			final Expression[] y = new Expression[n];
			
			idct(dct, y);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.toString(toDoubles(f)));
			Tools.debugPrint(Arrays.toString(toDoubles(dct)));
			Tools.debugPrint(Arrays.toString(toDoubles(y)));
		}
		
		{
			final Expression[][] f = {
					constants(1.0, 2.0, 3.0, 4.0),
					constants(2.0, 3.0, 4.0, 1.0),
					constants(3.0, 4.0, 1.0, 2.0),
					constants(4.0, 1.0, 2.0, 3.0),
			};
			final Expression[][] dct = dct(f);
			final Expression[][] y = idct(dct);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(f)));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(dct)));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(y)));
		}
		
		{
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
			final Expression[][][] dct = dct(f);
			final Expression[][][] y = idct(dct);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(f)));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(dct)));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(y)));
		}
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
	
	private static final Class<?> getDeepDoublesClass(final int n) {
		Class<?> result = double[].class;
		
		for (int i = 1; i < n; ++i) {
			result = Array.newInstance(result, 0).getClass();
		}
		
		return result;
	}
	
	public static final Expression[] constants(final double... values) {
		final int n = values.length;
		final Expression[] result = new Expression[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = constant(values[i]);
		}
		
		return result;
	}
	
	public static final Expression[] expressions(final Object... expressions) {
		final int n = expressions.length;
		final Expression[] result = new Expression[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = expression(expressions[i]);
		}
		
		return result;
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
		return transform(f, dimensionIndex, result, DCTc::dct);
	}
	
	public static final <T> T idct(final T f, final int dimensionIndex, final T result) {
		return transform(f, dimensionIndex, result, DCTc::idct);
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
				
				transformation.transform(inputBuffer, outputBuffer);
				
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
	
	/**
	 * @author codistmonk (creation 2015-03-25)
	 */
	public static abstract interface Transformation extends Serializable {
		
		public abstract void transform(Expression[] input, Expression[] output);
		
	}
	
	public static final Expression[] dct(final Expression[] f, final Expression[] result) {
		final int n = f.length;
		
		for (int k = 0; k < n; ++k) {
			result[k] = dct(f, k);
		}
		
		return result;
	}
	
	public static final Expression[] idct(final Expression[] dct, final Expression[] result) {
		final int n = dct.length;
		
		for (int x = 0; x < n; ++x) {
			result[x] = idct(dct, x);
		}
		
		return result;
	}
	
	public static final Expression dct(final Expression[] f, final int k) {
		final int n = f.length;
		Expression result = constant(0.0);
		
		if (k == 0) {
			for (int x = 0; x < n; ++x) {
				result = add(result, f[x]);
			}
		} else {
			for (int x = 0; x < n; ++x) {
				result = add(result, dct1k(f[x], x, k, n));
			}
			
			result = multiply(result, sqrt(2.0));
		}
		
		return divide(result, sqrt(n));
	}
	
	public static final Expression idct(final Expression[] dct, final Object x) {
		final int n = dct.length;
		Expression result = dct[0];
		
		for (int k = 1; k < n; ++k) {
			result = add(result, idct1k(dct[k], x, k, n));
		}
		
		return divide(result, sqrt(n));
	}
	
	public static final Expression dct1k(final Expression a, final Object x, final int k, final int n) {
		return multiply(a, cos(divide(multiply(add(multiply(2.0, x), 1.0), k, PI), 2 * n)));
	}
	
	public static final Expression idct1k(final Expression a, final Object x, final int k, final int n) {
		return multiply(dct1k(a, x, k, n), constant(sqrt(2.0)));
	}
	
	public static final Constant constant(final Object value) {
		return new Constant(((Number) value).doubleValue());
	}
	
	public static final Expression expression(final Object expression) {
		return expression instanceof Expression ? (Expression) expression : constant(expression);
	}
	
	public static final Cos cos(final Object operand) {
		return new Cos(expression(operand));
	}
	
	public static final Addition add(final Object... operands) {
		final int n = operands.length;
		Addition result = new Addition(expression(operands[0]), expression(operands[1]));
		
		for (int i = 2; i < n; ++i) {
			result = new Addition(result, expression(operands[i]));
		}
		
		return result;
	}
	
	public static final Multiplication multiply(final Object... operands) {
		final int n = operands.length;
		Multiplication result = new Multiplication(expression(operands[0]), expression(operands[1]));
		
		for (int i = 2; i < n; ++i) {
			result = new Multiplication(result, expression(operands[i]));
		}
		
		return result;
	}
	
	public static final Subtraction subtract(final Object leftOperand, final Object rightOperand) {
		return new Subtraction(expression(leftOperand), expression(rightOperand));
	}
	
	public static final Division divide(final Object leftOperand, final Object rightOperand) {
		return new Division(expression(leftOperand), expression(rightOperand));
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Cos extends UnaryOperation.Default {
		
		public Cos(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return Math.cos(this.getOperand().getAsDouble());
		}
		
		private static final long serialVersionUID = -8359113605516006482L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Addition extends BinaryOperation.Default {
		
		public Addition(final Expression leftOperand, final Expression rightOperand) {
			super(leftOperand, rightOperand);
		}
		
		@Override
		public final double getAsDouble() {
			return this.getLeftOperand().getAsDouble() + this.getRightOperand().getAsDouble();
		}
		
		private static final long serialVersionUID = 6536740395597475091L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Subtraction extends BinaryOperation.Default {
		
		public Subtraction(final Expression leftOperand, final Expression rightOperand) {
			super(leftOperand, rightOperand);
		}
		
		@Override
		public final double getAsDouble() {
			return this.getLeftOperand().getAsDouble() - this.getRightOperand().getAsDouble();
		}
		
		private static final long serialVersionUID = -7252500312383884584L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Multiplication extends BinaryOperation.Default {
		
		public Multiplication(final Expression leftOperand, final Expression rightOperand) {
			super(leftOperand, rightOperand);
		}
		
		@Override
		public final double getAsDouble() {
			return this.getLeftOperand().getAsDouble() * this.getRightOperand().getAsDouble();
		}
		
		private static final long serialVersionUID = 6537238820394469045L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Division extends BinaryOperation.Default {
		
		public Division(final Expression leftOperand, final Expression rightOperand) {
			super(leftOperand, rightOperand);
		}
		
		@Override
		public final double getAsDouble() {
			return this.getLeftOperand().getAsDouble() / this.getRightOperand().getAsDouble();
		}
		
		private static final long serialVersionUID = -3646307774259846699L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static abstract interface Expression extends DoubleSupplier, Serializable {
		//
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Constant implements Expression {
		
		private final double value;
		
		public Constant(final double value) {
			this.value = value;
		}
		
		@Override
		public final double getAsDouble() {
			return this.value;
		}
		
		private static final long serialVersionUID = 4701039521481142899L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static abstract interface UnaryOperation extends Expression {
		
		public abstract Expression getOperand();
		
		/**
		 * @author codistmonk (creation 2015-03-27)
		 */
		public static abstract class Default implements UnaryOperation {
			
			private final Expression operand;
			
			protected Default(final Expression operand) {
				this.operand = operand;
			}
			
			@Override
			public final Expression getOperand() {
				return this.operand;
			}
			
			private static final long serialVersionUID = 4857285485599960277L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static abstract interface BinaryOperation extends Expression {
		
		public abstract Expression getLeftOperand();
		
		public abstract Expression getRightOperand();
		
		/**
		 * @author codistmonk (creation 2015-03-27)
		 */
		public static abstract class Default implements BinaryOperation {
			
			private final Expression leftOperand;
			
			private final Expression rightOperand;
			
			protected Default(final Expression leftOperand, final Expression rightOperand) {
				this.leftOperand = leftOperand;
				this.rightOperand = rightOperand;
			}
			
			@Override
			public final Expression getLeftOperand() {
				return this.leftOperand;
			}
			
			@Override
			public final Expression getRightOperand() {
				return this.rightOperand;
			}
			
			private static final long serialVersionUID = -4709432671538857881L;
			
		}
		
	}
	
}
