package dct;

import static dct.DCTa.getDimensionCount;
import static imj3.tools.CommonTools.cartesian;
import static java.util.Collections.sort;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.deepClone;
import static net.sourceforge.aprog.tools.Tools.swap;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import dct.DCTc.BinaryOperation;
import dct.DCTc.Expression;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-23)
 */
public final class DCTc {
	
	private DCTc() {
		throw new IllegalInstantiationException();
	}
	
	public static final Constant ZERO = constant(0.0);
	
	public static final Constant ONE = constant(1.0);
	
	public static final Constant MINUS_ONE = constant(-1.0);
	
	public static final Constant PI = constant(Math.PI);
	
	public static final Expression HALF_PI = divide(PI, 2.0);
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final double epsilon = 1.0E-12;
		final Variable x = variable("x");
		final Variable y = variable("y");
		
		if (true) {
//			final double[] f = { 1.0, 2.0, 3.0, 4.0 };
//			final double[] f = { 121, 58, 61, 113, 171, 200, 226, 246 };
//			final double[] f = { 121, 58 };
			final Expression[] f = constants(121, 58);
			final int n = f.length;
			final Expression[] dct = new Expression[n];
			
			dct(f, dct);
			
			final Expression[] g = new Expression[n];
			
			idct(dct, g);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.toString(toDoubles(dct)));
			Tools.debugPrint(Arrays.toString(toDoubles(g)));
			
			Tools.debugPrint(contract(dct, DCTc::idct, expression(1.0)).approximated(epsilon).simplified());
			Tools.debugPrint(contract(dct, DCTc::idct, x).approximated(epsilon).simplified());
		}
		
		if (true) {
//			final Expression[][] f = {
//					constants(1.0, 2.0, 3.0, 4.0),
//					constants(2.0, 3.0, 4.0, 1.0),
//					constants(3.0, 4.0, 1.0, 2.0),
//					constants(4.0, 1.0, 2.0, 3.0),
//			};
			final Expression[][] f = {
					constants(1.0, 2.0),
					constants(2.0, 3.0),
			};
			final Expression[][] dct = dct(f);
			final Expression[][] g = idct(dct);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(dct)));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(g)));
			
			Tools.debugPrint(contract(dct, DCTc::idct, x, y).approximated(epsilon).simplified());
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
			final Expression[][][] dct = dct(f);
			final Expression[][][] g = idct(dct);
			
			Tools.debugPrint();
			Tools.debugPrint(Arrays.deepToString(f));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(dct)));
			Tools.debugPrint(Arrays.deepToString(deepToDoubles(g)));
			
			final Variable z = variable("z");
			Expression simplified = deepExpand(contract(dct, DCTc::idct, x, y, z)).simplified();
			simplified = add(terms(simplified).stream().map(DCTc::cosProductToSum).toArray()).simplified().reorder().simplified();
			Tools.debugPrint(simplified.approximated(epsilon).simplified());
			
			for (int i = 0; i <= 1; ++i) {
				for (int j = 0; j <= 1; ++j) {
					for (int k = 0; k <= 1; ++k) {
						x.setValue(i);
						y.setValue(j);
						z.setValue(k);
						
						Tools.debugPrint(i, j, k, simplified.getAsDouble());
					}
				}
			}
		}
	}
	
	public static final Expression cosProductToSum(final Expression expression) {
		final List<Expression> factors = factors(expression);
		final int n = factors.size();
		
		for (int i = 0; i < n; ++i) {
			final Cos cosI = cast(Cos.class, factors.get(i));
			
			if (cosI != null) {
				for (int j = i + 1; j < n; ++j) {
					final Cos cosJ = cast(Cos.class, factors.get(j));
					
					if (cosJ != null) {
						final Expression u = cosI.getOperand();
						final Expression v = cosJ.getOperand();
						final Expression c1 = cos(subtract(u, v));
						final Expression c2 = cos(add(u, v));
						
						factors.remove(j);
						factors.remove(i);
						
						final Expression r = multiply(factors.toArray());
						
						return multiply(0.5, add(cosProductToSum(multiply(r, c1)), cosProductToSum(multiply(r, c2))));
					}
				}
			}
		}
		
		return expression;
	}
	
	public static final Expression deepExpand(final Expression expression) {
		{
			final Multiplication multiplication = cast(Multiplication.class, expression);
			
			if (multiplication != null) {
				final List<Expression> leftTerms = terms(deepExpand(multiplication.getLeftOperand()));
				final List<Expression> rightTerms = terms(deepExpand(multiplication.getRightOperand()));
				
				return add(leftTerms.stream().flatMap(left -> rightTerms.stream().map(right -> multiply(left, right))).toArray());
			}
		}
		{
			final Division division = cast(Division.class, expression);
			
			if (division != null) {
				final List<Expression> leftTerms = terms(deepExpand(division.getLeftOperand()));
				final Expression right = deepExpand(division.getRightOperand());
				
				return add(leftTerms.stream().map(left -> divide(left, right)).toArray());
			}
		}
		
		{
			final UnaryOperation unary = cast(UnaryOperation.class, expression);
			
			if (unary != null) {
				return unary.maybeNew(deepExpand(unary.getOperand()));
			}
		}
		
		{
			final BinaryOperation binary = cast(BinaryOperation.class, expression);
			
			if (binary != null) {
				return binary.maybeNew(
						deepExpand(binary.getLeftOperand()), deepExpand(binary.getRightOperand()));
			}
		}
		
		return expression;
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
	
	public static final void deepSimplify(final Object array) {
		final Class<? extends Object> cls = array.getClass();
		final int n = Array.getLength(array);
		
		if (cls.getComponentType().isArray()) {
			for (int i = 0; i < n; ++i) {
				deepSimplify(Array.get(array, i));
			}
		} else {
			for (int i = 0; i < n; ++i) {
				Array.set(array, i, ((Expression) Array.get(array, i)).simplified());
			}
		}
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
	
	private static final Expression contract(final Object array, final Contraction contraction, final Expression... indices) {
		return contract(array, contraction, indices, 0);
	}
	
	private static final Expression contract(Object array, final Contraction contraction, final Expression[] indices, final int indexIndex) {
		final Expression[] buffer;
		
		if (indexIndex + 1 < indices.length) {
			final int n = Array.getLength(array);
			buffer = new Expression[n];
			
			for (int i = 0; i < n; ++i) {
				buffer[i] = contract(Array.get(array, i), contraction, indices, indexIndex + 1);
			}
		} else {
			buffer = (Expression[]) array;
		}
		
		return contraction.contract(buffer, indices[indexIndex]);
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
	
	/**
	 * @author codistmonk (creation 2015-03-25)
	 */
	public static abstract interface Contraction extends Serializable {
		
		public abstract Expression contract(Expression[] input, Expression index);
		
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
		
		return divide(result, sqrt(n)).simplified();
	}
	
	public static final Expression idct(final Expression[] dct, final Object x) {
		final int n = dct.length;
		Expression result = dct[0];
		
		for (int k = 1; k < n; ++k) {
			result = add(result, idct1k(dct[k], x, k, n));
		}
		
		return divide(result, sqrt(n)).simplified();
	}
	
	public static final Expression dct1k(final Expression a, final Object x, final int k, final int n) {
		return multiply(a, cos(divide(multiply(add(multiply(2.0, x), 1.0), k, PI), 2 * n))).simplified();
	}
	
	public static final Expression idct1k(final Expression a, final Object x, final int k, final int n) {
		return multiply(dct1k(a, x, k, n), sqrt(2.0)).simplified();
	}
	
	public static final Variable variable(final Object name) {
		return new Variable(name.toString());
	}
	
	public static final Constant constant(final Object value) {
		return new Constant(((Number) value).doubleValue());
	}
	
	public static final Expression expression(final Object expression) {
		return expression instanceof Expression ? (Expression) expression :
			expression instanceof CharSequence ? variable(expression) : constant(expression);
	}
	
	public static final Expression abs(final Object expression) {
		return new Abs(expression(expression));
	}
	
	public static final Expression sqrt(final Object expression) {
		return new Sqrt(expression(expression));
	}
	
	public static final Expression cos(final Object operand) {
		return new Cos(expression(operand));
	}
	
	public static final Expression negate(final Object operand) {
		return new Negation(expression(operand));
	}
	
	public static final Expression add(final Object... operands) {
		final int n = operands.length;
		
		if (n == 0) {
			return ZERO;
		}
		
		if (n == 1) {
			return expression(operands[0]);
		}
		
		Addition result = new Addition(expression(operands[0]), expression(operands[1]));
		
		for (int i = 2; i < n; ++i) {
			result = new Addition(result, expression(operands[i]));
		}
		
		return result;
	}
	
	public static final Expression multiply(final Object... operands) {
		final int n = operands.length;
		
		if (n == 0) {
			return ONE;
		}
		
		if (n == 1) {
			return expression(operands[0]);
		}
		
		Multiplication result = new Multiplication(expression(operands[0]), expression(operands[1]));
		
		for (int i = 2; i < n; ++i) {
			result = new Multiplication(result, expression(operands[i]));
		}
		
		return result;
	}
	
	public static final Expression subtract(final Object leftOperand, final Object rightOperand) {
//		return new Subtraction(expression(leftOperand), expression(rightOperand));
		return add(leftOperand, multiply(MINUS_ONE, rightOperand));
	}
	
	public static final Expression divide(final Object leftOperand, final Object rightOperand) {
		return new Division(expression(leftOperand), expression(rightOperand));
	}
	
	public static final List<Expression> terms(final Expression expression) {
		final List<Expression> result = new ArrayList<>();
		final Addition addition = cast(Addition.class, expression);
		
		if (addition != null) {
			result.addAll(terms(addition.getLeftOperand()));
			result.addAll(terms(addition.getRightOperand()));
		} else {
			result.add(expression);
		}
		
		sort(result);
		
		return result;
	}
	
	public static final List<Expression> factors(final Expression expression) {
		final List<Expression> result = new ArrayList<>();
		final Multiplication multiplication = cast(Multiplication.class, expression);
		
		if (multiplication != null) {
			result.addAll(factors(multiplication.getLeftOperand()));
			result.addAll(factors(multiplication.getRightOperand()));
		} else {
			result.add(expression);
		}
		
		sort(result);
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Negation extends UnaryOperation.Default {
		
		public Negation(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return -this.getOperand().getAsDouble();
		}
		
		@Override
		public final Expression simplified(final Expression simplifiedOperand) {
			final Negation negativeOperand = cast(this.getClass(), simplifiedOperand);
			
			if (negativeOperand != null) {
				return negativeOperand.getOperand();
			}
			
			final Constant constantOperand = cast(Constant.class, simplifiedOperand);
			
			if (constantOperand != null) {
				return constant(-constantOperand.getAsDouble());
			}
			
			return super.simplified(simplifiedOperand);
		}
		
		@Override
		public final String toString() {
			return "-" + this.getOperand();
		}
		
		private static final long serialVersionUID = -117667692146333768L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Sqrt extends UnaryOperation.Default {
		
		public Sqrt(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return Math.sqrt(this.getOperand().getAsDouble());
		}
		
		@Override
		public final Expression simplified(final Expression simplifiedOperand) {
			if (ZERO.equals(simplifiedOperand)) {
				return ZERO;
			}
			
			if (ONE.equals(simplifiedOperand)) {
				return ONE;
			}
			
			final Multiplication multiplicationOperand = cast(Multiplication.class, simplifiedOperand);
			
			if (multiplicationOperand != null) {
				final Expression left = multiplicationOperand.getLeftOperand();
				final Expression right = multiplicationOperand.getRightOperand();
				
				if (left.equals(right)) {
					return abs(left).simplified();
				}
			}
			
			final Constant constant = cast(Constant.class, simplifiedOperand);
			
			if (constant != null) {
				final double valueAsDouble = constant.getAsDouble();
				final double sqrt = Math.sqrt(valueAsDouble);
				
				if (sqrt * sqrt == valueAsDouble) {
					return constant(sqrt);
				}
			}
			
			return super.simplified(simplifiedOperand);
		}
		
		@Override
		public final String toString() {
			return "sqrt(" + this.getOperand() + ")";
		}
		
		private static final long serialVersionUID = -4589762704500366742L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Abs extends UnaryOperation.Default {
		
		public Abs(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return Math.abs(this.getOperand().getAsDouble());
		}
		
		@Override
		public final Expression simplified(final Expression simplifiedOperand) {
			final Constant constantOperand = cast(Constant.class, simplifiedOperand);
			
			if (constantOperand != null) {
				return constant(this.getAsDouble());
			}
			
			return super.simplified(simplifiedOperand);
		}
		
		@Override
		public final String toString() {
			return "abs(" + this.getOperand() + ")";
		}
		
		private static final long serialVersionUID = -7189015218426712149L;
		
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
		
		@Override
		public final Expression simplified(final Expression simplifiedOperand) {
			if (ZERO.equals(simplifiedOperand)) {
				return ONE;
			}
			
			if (PI.equals(simplifiedOperand)) {
				return MINUS_ONE;
			}
			
			if (HALF_PI.equals(simplifiedOperand)) {
				return ZERO;
			}
			
			return super.simplified(simplifiedOperand);
		}
		
		@Override
		public final String toString() {
			return "cos(" + this.getOperand() + ")";
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
		public final Addition reorder() {
			final Addition superResult = (Addition) super.reorder();
			final Addition candidate = (Addition) add(terms(superResult).toArray());
			
			return this.equals(candidate) ? superResult : candidate;
		}
		
		@Override
		public final double getAsDouble() {
			return this.getLeftOperand().getAsDouble() + this.getRightOperand().getAsDouble();
		}
		
		@Override
		public final Expression simplified(final Expression simplifiedLeftOperand, final Expression simplifiedRightOperand) {
			if (ZERO.equals(simplifiedLeftOperand)) {
				return simplifiedRightOperand;
			}
			
			if (ZERO.equals(simplifiedRightOperand)) {
				return simplifiedLeftOperand;
			}
			
			{
				final Constant constantLeft = cast(Constant.class, simplifiedLeftOperand);
				final Constant constantRight = cast(Constant.class, simplifiedRightOperand);
				
				if (constantLeft != null && constantRight != null) {
					return constant(this.getAsDouble());
				}
			}
			
			return super.simplified(simplifiedLeftOperand, simplifiedRightOperand);
		}
		
		@Override
		public final String toString() {
			return "(" + this.getLeftOperand() + "+" + this.getRightOperand() + ")";
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
		
		@Override
		public final Expression simplified(final Expression simplifiedLeftOperand, final Expression simplifiedRightOperand) {
			if (ZERO.equals(simplifiedRightOperand)) {
				return simplifiedLeftOperand;
			}
			
			if (ZERO.equals(simplifiedLeftOperand)) {
				return negate(simplifiedRightOperand).simplified();
			}
			
			if (simplifiedLeftOperand.equals(simplifiedRightOperand)) {
				return ZERO;
			}
			
			final Constant constantLeft = cast(Constant.class, simplifiedLeftOperand);
			final Constant constantRight = cast(Constant.class, simplifiedRightOperand);
			
			if (constantLeft != null && constantRight != null) {
				return constant(this.getAsDouble());
			}
			
			if (constantRight != null) {
				return add(simplifiedLeftOperand, constant(-constantRight.getAsDouble())).simplified();
			}
			
			return super.simplified(simplifiedLeftOperand, simplifiedRightOperand);
		}
		
		@Override
		public final String toString() {
			if (this.getLeftOperand() instanceof Division) {
				Tools.debugPrint(this);
			}
			return "(" + this.getLeftOperand() + "-" + this.getRightOperand() + ")";
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
		public final Multiplication reorder() {
			final Multiplication superResult = (Multiplication) super.reorder();
			final Multiplication candidate = (Multiplication) multiply(factors(superResult).toArray());
			
			return this.equals(candidate) ? superResult : candidate;
		}
		
		@Override
		public final double getAsDouble() {
			return this.getLeftOperand().getAsDouble() * this.getRightOperand().getAsDouble();
		}
		
		@Override
		public final Expression simplified(final Expression simplifiedLeftOperand, final Expression simplifiedRightOperand) {
			if (ZERO.equals(simplifiedLeftOperand) || ZERO.equals(simplifiedRightOperand)) {
				return ZERO;
			}
			
			if (ONE.equals(simplifiedLeftOperand)) {
				return simplifiedRightOperand;
			}
			
			if (ONE.equals(simplifiedRightOperand)) {
				return simplifiedLeftOperand;
			}
			
			final Sqrt sqrtLeft = cast(Sqrt.class, simplifiedLeftOperand);
			final Sqrt sqrtRight = cast(Sqrt.class, simplifiedRightOperand);
			
			if (sqrtLeft != null && sqrtLeft.equals(sqrtRight)) {
				return sqrtLeft.getOperand();
			}
			
			final Division divisionRight = cast(Division.class, simplifiedRightOperand);
			
			if (divisionRight != null) {
				final Expression simplifiedNumerator = tryToSimplifyMultiplication(simplifiedLeftOperand, divisionRight.getLeftOperand());
				
				if (simplifiedNumerator != null) {
					return divide(simplifiedNumerator, divisionRight.getRightOperand()).simplified();
				}
			}
			
			final Constant constantLeft = cast(Constant.class, simplifiedLeftOperand);
			final Constant constantRight = cast(Constant.class, simplifiedRightOperand);
			
			if (constantLeft != null && constantRight != null) {
				return constant(this.getAsDouble());
			}
			
			return super.simplified(simplifiedLeftOperand, simplifiedRightOperand);
		}
		
		@Override
		public final Expression approximated(final double epsilon, final Expression approximatedLeftOperand,
				final Expression approximatedRightOperand, final Expression defaultResult) {
			{
				final List<Expression> leftTerms = terms(approximatedLeftOperand);
				final List<Expression> rightTerms = terms(approximatedRightOperand);
				
				if (1 < leftTerms.size() || 1 < rightTerms.size()) {
					final List<Expression> distributed = new ArrayList<DCTc.Expression>();
					
					for (final Expression leftTerm : leftTerms) {
						for (final Expression rightTerm : rightTerms) {
							distributed.addAll(terms(multiply(leftTerm, rightTerm).approximated(epsilon)));
						}
					}
					
					reorder(distributed);
					
					return add(distributed.toArray()).approximated(epsilon);
				}
			}
			
			{
				final List<Expression> factors = factors(approximatedLeftOperand);
				
				factors.addAll(factors(approximatedRightOperand));
				
				final int n = factors.size();
				
				if (2 < n) {
					reorder(factors);
					
					return multiply(factors.toArray()).simplified();
				}
			}
			
			
			return super.approximated(epsilon, approximatedLeftOperand, approximatedRightOperand, defaultResult);
		}
		
		private static final void reorder(final List<Expression> expressions) {
			final int n = expressions.size();
			
			for (int i = 0, j = n - 1; i < j;) {
				if (expressions.get(i) instanceof Constant) {
					++i;
				} else {
					Collections.swap(expressions, i, j);
					--j;
				}
			}
		}
		
		@Override
		public final String toString() {
			return "(" + this.getLeftOperand() + "*" + this.getRightOperand() + ")";
		}
		
		private static final long serialVersionUID = 6537238820394469045L;
		
		public static final Expression tryToSimplifyMultiplication(final Expression leftOperand, final Expression rightOperand) {
			final Expression multiplication = multiply(leftOperand, rightOperand);
			final Expression simplified = multiplication.simplified();
			
			return multiplication == simplified ? null : simplified;
		}
		
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
		
		@Override
		public final Expression simplified(final Expression simplifiedLeftOperand, final Expression simplifiedRightOperand) {
			if (ONE.equals(simplifiedRightOperand)) {
				return simplifiedLeftOperand;
			}
			
			if (!ZERO.equals(simplifiedRightOperand)) {
				if (ZERO.equals(simplifiedLeftOperand)) {
					return ZERO;
				}
				
				if (simplifiedLeftOperand.equals(simplifiedRightOperand)) {
					return ONE;
				}
			}
			
			{
				final Division divisionLeft = cast(this.getClass(), simplifiedLeftOperand);
				
				if (divisionLeft != null) {
					final Expression simplifiedDenominator = Multiplication.tryToSimplifyMultiplication(divisionLeft.getRightOperand(), simplifiedRightOperand);
					
					if (simplifiedDenominator != null) {
						return divide(divisionLeft.getLeftOperand(), simplifiedDenominator).simplified();
					}
				}
			}
			
			simplify_terms:
			{
				final List<Expression> termsLeft = terms(simplifiedLeftOperand);
				final int n = termsLeft.size();
				
				if (2 <= n) {
					for (int i = 0; i < n; ++i) {
						final Expression simplifiedTerm = tryToSimplifyDivision(termsLeft.get(i), simplifiedRightOperand);
						
						if (simplifiedTerm != null) {
							termsLeft.set(i, simplifiedTerm);
						} else {
							break simplify_terms;
						}
					}
					
					return add(termsLeft.toArray()).simplified();
				}
			}
			
			{
				final List<Expression> factorsLeft = factors(simplifiedLeftOperand);
				final int n = factorsLeft.size();
				
				if (2 <= n) {
					for (int i = 0; i < n; ++i) {
						final Expression simplifiedFactor = tryToSimplifyDivision(factorsLeft.get(i), simplifiedRightOperand);
						
						if (simplifiedFactor != null) {
							factorsLeft.set(i, simplifiedFactor);
							
							return multiply(factorsLeft.toArray()).simplified();
						}
					}
				}
			}
			
			{
				final Constant constantLeft = cast(Constant.class, simplifiedLeftOperand);
				final Constant constantRight = cast(Constant.class, simplifiedRightOperand);
				
				if (constantLeft != null && constantRight != null) {
					final double leftAsDouble = constantLeft.getAsDouble();
					final double rightAsDouble = constantRight.getAsDouble();
					
					final double value = leftAsDouble / rightAsDouble;
					
					if (value * rightAsDouble == leftAsDouble) {
						return constant(value);
					}
					
					final long leftAsLong = (long) leftAsDouble;
					final long rightAsLong = (long) rightAsDouble;
					
					if (leftAsDouble == leftAsLong && rightAsDouble == rightAsLong) {
						final long gcd = MathTools.gcd(leftAsLong, rightAsLong);
						
						if (gcd == rightAsLong) {
							return constant(leftAsLong / gcd);
						}
						
						if (gcd != 1L) {
							return divide(leftAsLong / gcd, rightAsLong / gcd);
						}
					}
				}
			}
			
			return super.simplified(simplifiedLeftOperand, simplifiedRightOperand);
		}
		
		@Override
		public final String toString() {
			return "(" + this.getLeftOperand() + "/" + this.getRightOperand() + ")";
		}
		
		private static final long serialVersionUID = -3646307774259846699L;
		
		public static final Expression tryToSimplifyDivision(final Expression leftOperand, final Expression rightOperand) {
			final Expression division = divide(leftOperand, rightOperand);
			final Expression simplified = division.simplified();
			
			return division == simplified ? null : simplified;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static abstract interface Expression extends DoubleSupplier, Serializable, Comparable<Expression> {
		
		public default Expression simplified() {
			return this;
		}
		
		public default Expression approximated(double epsilon) {
			return this;
		}
		
		public default Expression reorder() {
			return this;
		}
		
		@Override
		public default int compareTo(final Expression other) {
			return this.getClass().getName().compareTo(other.getClass().getName());
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Variable implements Expression {
		
		private final String name;
		
		private double value;
		
		public Variable(final String name) {
			this.name = name;
		}
		
		public Variable setValue(final double value) {
			this.value = value;
			
			return this;
		}
		
		@Override
		public final double getAsDouble() {
			return this.value;
		}
		
		@Override
		public final int compareTo(final Expression other) {
			final Variable that = cast(this.getClass(), other);
			
			if (that != null) {
				return this.toString().compareTo(that.toString());
			}
			
			return Expression.super.compareTo(other);
		}
		
		@Override
		public final String toString() {
			return this.name;
		}
		
		private static final long serialVersionUID = -4287494233694126598L;
		
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
		
		@Override
		public final int compareTo(final Expression other) {
			final Constant that = cast(this.getClass(), other);
			
			if (that != null) {
				return Double.compare(this.getAsDouble(), that.getAsDouble());
			}
			
			return Expression.super.compareTo(other);
		}
		
		@Override
		public final int hashCode() {
			return Double.hashCode(this.getAsDouble());
		}
		
		@Override
		public final boolean equals(final Object object) {
			final Constant that = cast(this.getClass(), object);
			
			return that != null && this.getAsDouble() == that.getAsDouble();
		}
		
		@Override
		public final String toString() {
			return String.format(Locale.ENGLISH, "%.2f", this.getAsDouble());
		}
		
		private static final long serialVersionUID = 4701039521481142899L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static abstract interface UnaryOperation extends Expression {
		
		@Override
		public default UnaryOperation reorder() {
			if (this.getClass() == Cos.class) {
				Tools.debugPrint(this.getOperand().reorder().simplified());
			}
			return this.maybeNew(this.getOperand().reorder());
		}
		
		public abstract Expression getOperand();
		
		@Override
		public default Expression simplified() {
			return this.simplified(this.getOperand().simplified());
		}
		
		public default Expression simplified(final Expression simplifiedOperand) {
			if (this.getOperand() != simplifiedOperand) {
				return this.newInstance(simplifiedOperand);
			}
			
			return Expression.super.simplified();
		}
		
		public default UnaryOperation maybeNew(final Expression operand) {
			return this.getOperand() == operand ? this : this.newInstance(operand);
		}
		
		public default UnaryOperation newInstance(final Expression operand) {
			try {
				return this.getClass().getConstructor(Expression.class).newInstance(operand);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		@Override
		public default Expression approximated(final double epsilon) {
			final Expression approximatedOperand = this.getOperand().approximated(epsilon);
			
			try {
				final UnaryOperation approximated = this.newInstance(approximatedOperand);
				
				if (approximatedOperand instanceof Constant) {
					final double value = approximated.getAsDouble();
					
					return Math.abs(value) < epsilon ? ZERO : constant(value);
				}
				
				return this.approximated(epsilon, approximatedOperand, approximated);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		public default Expression approximated(final double epsilon, final Expression approximatedOperand, final Expression defaultResult) {
			return defaultResult;
		}
		
		@Override
		public default int compareTo(final Expression other) {
			final UnaryOperation that = cast(this.getClass(), other);
			
			if (that != null) {
				return this.getOperand().compareTo(that.getOperand());
			}
			
			return Expression.super.compareTo(other);
		}
		
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
			
			@Override
			public final int hashCode() {
				return this.getOperand().hashCode() + this.getClass().hashCode();
			}
			
			@Override
			public final boolean equals(final Object object) {
				final Default that = cast(this.getClass(), object);
				
				return that != null && this.getOperand().equals(that.getOperand());
			}
			
			private static final long serialVersionUID = 4857285485599960277L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static abstract interface BinaryOperation extends Expression {
		
		@Override
		public default BinaryOperation reorder() {
			return this.maybeNew(this.getLeftOperand().reorder(), this.getRightOperand().reorder());
		}
		
		public abstract Expression getLeftOperand();
		
		public abstract Expression getRightOperand();
		
		@Override
		public default Expression simplified() {
			return this.simplified(this.getLeftOperand().simplified(), this.getRightOperand().simplified());
		}
		
		public default Expression simplified(final Expression simplifiedLeftOperand, final Expression simplifiedRightOperand) {
			final Expression newInstance = this.maybeNew(simplifiedLeftOperand, simplifiedRightOperand);
			
			return this != newInstance ? newInstance : Expression.super.simplified();
		}
		
		public default BinaryOperation maybeNew(final Expression leftOperand, final Expression rightOperand) {
			return this.getLeftOperand() == leftOperand && this.getRightOperand() == rightOperand ? this :
				this.newInstance(leftOperand, rightOperand);
		}
		
		public default BinaryOperation newInstance(final Expression leftOperand, final Expression rightOperand) {
			try {
				return this.getClass().getConstructor(Expression.class, Expression.class).newInstance(leftOperand, rightOperand);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		@Override
		public default Expression approximated(final double epsilon) {
			final Expression approximatedLeftOperand = this.getLeftOperand().approximated(epsilon);
			final Expression approximatedRightOperand = this.getRightOperand().approximated(epsilon);
			
			try {
				final BinaryOperation approximated = this.newInstance(approximatedLeftOperand, approximatedRightOperand);
				
				if (approximatedLeftOperand instanceof Constant && approximatedRightOperand instanceof Constant) {
					final double value = approximated.getAsDouble();
					
					return Math.abs(value) < epsilon ? ZERO : constant(value);
				}
				
				return this.approximated(epsilon, approximatedLeftOperand, approximatedRightOperand, approximated);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		public default Expression approximated(final double epsilon, final Expression approximatedLeftOperand, final Expression approximatedRightOperand, final Expression defaultResult) {
			return defaultResult;
		}
		
		@Override
		public default int compareTo(final Expression other) {
			final BinaryOperation that = cast(this.getClass(), other);
			
			if (that != null) {
				final int leftComparison = this.getLeftOperand().compareTo(that.getRightOperand());
				
				if (leftComparison != 0) {
					return leftComparison;
				}
				
				return this.getRightOperand().compareTo(that.getRightOperand());
			}
			
			return Expression.super.compareTo(other);
		}
		
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
			
			@Override
			public final int hashCode() {
				return this.getLeftOperand().hashCode() + this.getRightOperand().hashCode() + this.getClass().hashCode();
			}
			
			@Override
			public final boolean equals(final Object object) {
				final Default that = cast(this.getClass(), object);
				
				return that != null && this.getLeftOperand().equals(that.getLeftOperand()) && this.getRightOperand().equals(that.getRightOperand());
			}
			
			private static final long serialVersionUID = -4709432671538857881L;
			
		}
		
	}
	
}
