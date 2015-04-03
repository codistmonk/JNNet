package jnnet3.dct;

import static java.lang.Math.min;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static net.sourceforge.aprog.tools.Tools.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-03-28)
 */
public final class MiniCAS {
	
	private MiniCAS() {
		throw new IllegalInstantiationException();
	}
	
	public static final Constant ZERO = constant(0.0);
	
	public static final Constant ONE = constant(1.0);
	
	public static final Constant MINUS_ONE = constant(-1.0);
	
	public static final Constant PI = constant(Math.PI);
	
	public static final Expression HALF_PI = divide(PI, 2.0);
	
	public static final Expression TWO_PI = multiply(PI, 2.0);
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		debugPrint(add(1, 5, add("x", 4), add(2)).accept(Canonicalize.INSTANCE));
	}
	
	public static final Variable variable(final Object name) {
		return new Variable(name.toString());
	}
	
	public static final Constant constant(final Object value) {
		return value instanceof Constant ? (Constant) value :
			new Constant(value instanceof Expression ? ((Expression) value).getAsDouble() :
				((Number) value).doubleValue());
	}
	
	public static final Expression expression(final Object expression) {
		return expression instanceof Expression ? (Expression) expression :
			expression instanceof CharSequence ? variable(expression) : constant(expression);
	}
	
	public static final Sqrt sqrt(final Object expression) {
		return new Sqrt(expression(expression));
	}
	
	public static final Cos cos(final Object operand) {
		return new Cos(expression(operand));
	}
	
	public static final Expression negate(final Object operand) {
		return multiply(MINUS_ONE, operand);
	}
	
	public static final Inverse invert(final Object operand) {
		return new Inverse(expression(operand));
	}
	
	public static final Sum add(final Object... operands) {
		final Sum result = new Sum();
		
		result.getOperands().addAll(Arrays.stream(operands).map(MiniCAS::expression).collect(toList()));
		
		return result;
	}
	
	public static final Product multiply(final Object... operands) {
		final Product result = new Product();
		
		result.getOperands().addAll(Arrays.stream(operands).map(MiniCAS::expression).collect(toList()));
		
		return result;
	}
	
	public static final Expression subtract(final Object leftOperand, final Object rightOperand) {
		return add(leftOperand, negate(rightOperand));
	}
	
	public static final Expression divide(final Object leftOperand, final Object rightOperand) {
		return multiply(leftOperand, invert(rightOperand));
	}
	
	public static final boolean sameElements(final Collection<?> list1, final Collection<?> list2) {
		final int n = list1.size();
		
		if (n != list2.size()) {
			return false;
		}
		
		for (final Iterator<?> i = list1.iterator(), j = list2.iterator(); i.hasNext();) {
			if (i.next() != j.next()) {
				return false;
			}
		}
		
		return true;
	}
	
	public static final Expression[] constants(final double... values) {
		final int n = values.length;
		final Expression[] result = new Expression[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = constant(values[i]);
		}
		
		return result;
	}
	
	public static final Expression[] constants(final Expression... values) {
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
	
	@SuppressWarnings("unchecked")
	public static final Expression approximate(final Expression expression, final double epsilon) {
		return limitOf(expression, Canonicalize.INSTANCE, new Approximate(epsilon));
	}
	
	public static final Expression limitOf(final Expression expression, final Expression.Visitor<Expression>... rewriters) {
		Expression result = expression;
		Expression tmp;
		
		do {
			tmp = result;
			
			for (final Expression.Visitor<Expression> rewriter : rewriters) {
				result = result.accept(rewriter);
			}
		} while (!tmp.equals(result));
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Approximate implements Expression.Rewriter {
		
		private final double epsilon;
		
		public Approximate(final double epsilon) {
			this.epsilon = epsilon;
		}
		
		@Override
		public final Expression visit(final Constant constant) {
			return constant(this.approximate(constant.getAsDouble()));
		}
		
		@Override
		public final Expression visit(final UnaryOperation operation) {
			final Expression approximatedOperand = operation.getOperand().accept(this);
			
			if (approximatedOperand instanceof Constant) {
				return constant(this.approximate(operation.maybeNew(approximatedOperand).getAsDouble()));
			}
			
			return operation.maybeNew(approximatedOperand);
		}
		
		@Override
		public final Expression visit(final NaryOperation operation) {
			final List<Expression> approximatedOperands = operation.getOperands().stream().map(this).collect(toCollection(ArrayList::new));
			boolean allConstant = true;
			
			for (final Expression approximatedOperand : approximatedOperands) {
				if (!(approximatedOperand instanceof Constant)) {
					allConstant = false;
					break;
				}
			}
			
			if (allConstant) {
				return constant(this.approximate(operation.maybeNew(approximatedOperands).getAsDouble()));
			}
			
			if (operation.isCommutative()) {
				sort(approximatedOperands);
				int firstNonconstant = 0;
				
				while (firstNonconstant < approximatedOperands.size() && approximatedOperands.get(firstNonconstant) instanceof Constant) {
					++firstNonconstant;
				}
				
				final List<Expression> prefix = approximatedOperands.subList(0, firstNonconstant);
				final Constant c = constant(this.approximate(operation.maybeNew(prefix).getAsDouble()));
				
				prefix.clear();
				
				{
					final Product product = cast(Product.class, operation);
					
					if (product != null) {
						if (ZERO.equals(c)) {
							return ZERO;
						}
						
						if (!ONE.equals(c)) {
							prefix.add(c);
						}
					}
				}
				
				{
					final Sum sum = cast(Sum.class, operation);
					
					if (sum != null) {
						if (!ZERO.equals(c)) {
							prefix.add(c);
						}
					}
				}
			}
			
			return operation.maybeNew(approximatedOperands);
		}
		
		public final double approximate(final double value) {
			return Math.abs(value) < this.epsilon ? 0.0 : value;
		}
		
		private static final long serialVersionUID = 8471721004761825219L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Canonicalize implements Expression.Rewriter {
		
		@Override
		public final Expression visit(final UnaryOperation operation) {
			final Expression operand = operation.getOperand();
			final Expression canonicalOperand = operand.accept(this);
			
			{
				final Inverse inverse = cast(Inverse.class, operation);
				
				if (inverse != null) {
					final Inverse inverseOperand = cast(Inverse.class, canonicalOperand);
					
					if (inverseOperand != null) {
						return inverseOperand.getOperand();
					}
				}
			}
			
			// TODO handle more operations
			
			if (operand != canonicalOperand) {
				return operation.newInstance(canonicalOperand);
			}
			
			return operation;
		}
		
		@Override
		public final Expression visit(final NaryOperation operation) {
			final List<Expression> operands = operation.getOperands();
			final List<Expression> canonicalOperands = operands.stream().map(this).collect(toCollection(ArrayList::new));
			final List<Expression> flattened;
			
			if (operation.isAssociative()) {
				flattened = new ArrayList<>();
				
				for (final Expression operand : canonicalOperands) {
					if (operand.getClass() == operation.getClass()) {
						flattened.addAll(((NaryOperation) operand).getOperands());
					} else {
						flattened.add(operand);
					}
				}
			} else {
				flattened = canonicalOperands;
			}
			
			if (operation.isCommutative()) {
				sort(flattened);
			}
			
			{
				final Product product = cast(Product.class, operation);
				
				if (product != null) {
					Sum sum = null;
					final List<Expression> factors = new ArrayList<>();
					final List<Expression> terms = new ArrayList<>();
					
					for (final Expression factor : flattened) {
						if (factor instanceof Sum) {
							if (sum != null) {
								if (!factors.isEmpty()) {
									factors.add(0, null);
									
									for (final Expression expression : sum.getOperands()) {
										factors.set(0, expression);
										terms.add(product.maybeNew(factors));
									}
									
									factors.clear();
									sum = add(terms.toArray());
									terms.clear();
								}
								
								{
									factors.add(null);
									factors.add(null);
									
									for (final Expression left : sum.getOperands()) {
										factors.set(0, left);
										
										for (final Expression right : ((Sum) factor).getOperands()) {
											factors.set(1, right);
											terms.add(product.maybeNew(factors));
										}
									}
									
									factors.clear();
									sum = add(terms.toArray());
									terms.clear();
								}
							} else if (!factors.isEmpty()) {
								final int n = factors.size();
								
								factors.add(null);
								
								for (final Expression e : ((Sum) factor).getOperands()) {
									factors.set(n, e);
									terms.add(product.maybeNew(factors));
								}
								
								factors.clear();
								sum = add(terms.toArray());
								terms.clear();
							} else {
								sum = (Sum) factor;
							}
						} else {
							factors.add(factor);
						}
					}
					if (sum != null) {
						if (!factors.isEmpty()) {
							factors.add(0, null);
							
							for (final Expression expression : sum.getOperands()) {
								factors.set(0, expression);
								terms.add(product.maybeNew(factors));
							}
							
							factors.clear();
							sum = add(terms.toArray());
							terms.clear();
						}
					} else {
						sum = add(product.maybeNew(factors));
						factors.clear();
					}
					
					return strip(sum);
				}
			}
			
			if (flattened.size() == 1 && (operation instanceof Sum || operation instanceof Product)) {
				return strip(flattened.get(0));
			}
			
			return operation.maybeNew(flattened);
		}
		
		private static final long serialVersionUID = 1135894630542113318L;
		
		public static final Canonicalize INSTANCE = new Canonicalize();
		
		public static final Expression strip(final Expression expression) {
			Expression result = expression;
			
			while ((result instanceof Sum || result instanceof Product) && ((NaryOperation) result).getOperands().size() == 1) {
				result = ((NaryOperation) result).getOperands().get(0);
			}
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-28)
	 */
	public static abstract interface Expression extends Serializable, DoubleSupplier, Comparable<Expression> {
		
		public abstract <V> V accept(Visitor<V> visitor);
		
		public default boolean equals(final Expression other, final double epsilon) {
			return this.equals(other);
		}
		
		@Override
		public default int compareTo(final Expression other) {
			final boolean thisIsConstant = this instanceof Constant; 
			final boolean otherIsConstant = other instanceof Constant;
			
			if (thisIsConstant && !otherIsConstant) {
				return -1;
			}
			
			if (thisIsConstant && otherIsConstant) {
				return 0;
			}
			
			if (!thisIsConstant && otherIsConstant) {
				return +1;
			}
			
			return this.getType().compareTo(other.getType());
		}
		
		public default String getType() {
			return this.getClass().getName();
		}
		
		/**
		 * @author codistmonk (creation 2015-03-28)
		 * @param <V> 
		 */
		public static abstract interface Visitor<V> extends Serializable, Function<Expression, V> {
			
			@Override
			public default V apply(final Expression expression) {
				return expression.accept(this);
			}
			
			public default V visit(final Expression expression) {
				ignore(expression);
				
				return null;
			}
			
			public default V visit(final Variable variable) {
				return this.visit((Expression) variable);
			}
			
			public default V visit(final Constant constant) {
				return this.visit((Expression) constant);
			}
			
			public default V visit(final UnaryOperation operation) {
				return this.visit((Expression) operation);
			}
			
			public default V visit(final NaryOperation operation) {
				return this.visit((Expression) operation);
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2015-03-30)
		 */
		public static abstract interface Rewriter extends Visitor<Expression> {
			
			@Override
			public default Expression visit(final Expression expression) {
				return expression;
			}
			
			@Override
			public default Expression visit(final Variable variable) {
				return this.visit((Expression) variable);
			}
			
			@Override
			public default Expression visit(final Constant constant) {
				return this.visit((Expression) constant);
			}
			
			@Override
			public default Expression visit(final UnaryOperation operation) {
				return operation.maybeNew(operation.getOperand().accept(this));
			}
			
			@Override
			public default Expression visit(final NaryOperation operation) {
				return operation.maybeNew(
						operation.getOperands().stream().map(this).collect(toList()));
			}
			
		}
		
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
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public final double getAsDouble() {
			return this.value;
		}
		
		@Override
		public final int compareTo(final Expression other) {
			final Constant constantOther = cast(this.getClass(), other);
			
			if (constantOther != null) {
				return Double.compare(this.getAsDouble(), other.getAsDouble());
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
		public final boolean equals(final Expression other, final double epsilon) {
			final Constant that = cast(this.getClass(), other);
			
			return that != null && Math.abs(this.getAsDouble() - that.getAsDouble()) < epsilon;
		}
		
		@Override
		public final String toString() {
			if (DEBUG) {
				return Double.toString(this.getAsDouble());
			} else {
				return String.format(Locale.ENGLISH, "%.2f", this.getAsDouble());
			}
		}
		
		private static final long serialVersionUID = 4701039521481142899L;
		
		private static final boolean DEBUG = false;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-27)
	 */
	public static final class Variable implements Expression {
		
		private final String name;
		
		private Expression value;
		
		public Variable(final String name) {
			this.name = name;
		}
		
		public final Expression getValue() {
			return this.value;
		}
		
		public final Variable setValue(final Expression value) {
			this.value = value;
			
			return this;
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public final double getAsDouble() {
			return this.getValue().getAsDouble();
		}
		
		@Override
		public final int compareTo(final Expression other) {
			final Variable variableOther = cast(this.getClass(), other);
			
			if (variableOther != null) {
				return this.toString().compareTo(variableOther.toString());
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
	public static abstract interface UnaryOperation extends Expression {
		
		public abstract Expression getOperand();
		
		@Override
		public default <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public default int compareTo(final Expression other) {
			if (this.getClass() == other.getClass()) {
				return this.getOperand().compareTo(((UnaryOperation) other).getOperand());
			}
			
			return Expression.super.compareTo(other);
		}
		
		@Override
		public default boolean equals(final Expression other, final double epsilon) {
			final UnaryOperation that = cast(this.getClass(), other);
			
			return that != null && this.getOperand().equals(that.getOperand(), epsilon);
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
		
		/**
		 * @author codistmonk (creation 2015-03-27)
		 */
		public static abstract class Abstract implements UnaryOperation {
			
			private final Expression operand;
			
			protected Abstract(final Expression operand) {
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
				final Abstract that = cast(this.getClass(), object);
				
				return that != null && this.getOperand().equals(that.getOperand());
			}
			
			private static final long serialVersionUID = 4857285485599960277L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-28)
	 */
	public static abstract interface NaryOperation extends Expression {
		
		public abstract String getOperator();
		
		public abstract List<Expression> getOperands();
		
		public default boolean isCommutative() {
			return true;
		}
		
		public default boolean isAssociative() {
			return true;
		}
		
		@Override
		public default <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public default int compareTo(final Expression other) {
			if (this.getClass() == other.getClass()) {
				final NaryOperation commutativeAssociativeOther = (NaryOperation) other;
				final List<Expression> thisOperands = this.getOperands();
				final List<Expression> otherOperands = commutativeAssociativeOther.getOperands();
				final int n1 = thisOperands.size();
				final int n2 = otherOperands.size();
				final int n = min(n1, n2);
				
				for (int i = 0; i < n; ++i) {
					final int elementComparison = thisOperands.get(i).compareTo(otherOperands.get(i));
					
					if (elementComparison != 0) {
						return elementComparison;
					}
				}
				
				return n1 - n2;
			}
			
			return Expression.super.compareTo(other);
		}
		
		@Override
		public default boolean equals(final Expression other, final double epsilon) {
			final NaryOperation that = cast(this.getClass(), other);
			
			if (that == null) {
				return false;
			}
			
			final List<Expression> thisOperands = this.getOperands();
			final List<Expression> thatOperands = that.getOperands();
			final int n = thisOperands.size();
			
			if (n != thatOperands.size()) {
				return false;
			}
			
			for (int i = 0; i < n; ++i) {
				if (!thisOperands.get(i).equals(thatOperands.get(i), epsilon)) {
					return false;
				}
			}
			
			return true;
		}
		
		public default NaryOperation maybeNew(final Collection<Expression> operands) {
			return sameElements(this.getOperands(), operands) ? this : this.newInstance(operands);
		}
		
		public default NaryOperation newInstance(final Collection<Expression> operands) {
			try {
				final NaryOperation result = this.getClass().newInstance();
				
				result.getOperands().addAll(operands);
				
				return result;
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		/**
		 * @author codistmonk (creation 2015-03-27)
		 */
		public static abstract class Abstract implements NaryOperation {
			
			private final List<Expression> operands = new ArrayList<>();
			
			@Override
			public final List<Expression> getOperands() {
				return this.operands;
			}
			
			@Override
			public final int hashCode() {
				return this.getOperands().hashCode() + this.getClass().hashCode();
			}
			
			@Override
			public final boolean equals(final Object object) {
				final Abstract that = cast(this.getClass(), object);
				
				return that != null && this.getOperands().equals(that.getOperands());
			}
			
			@Override
			public final String toString() {
				return "(" + join(this.getOperator(), this.getOperands()) + ")";
			}
			
			private static final long serialVersionUID = 4857285485599960277L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Cos extends UnaryOperation.Abstract {
		
		public Cos(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return Math.cos(this.getOperand().getAsDouble());
		}
		
		@Override
		public final String toString() {
			return "(cos " + this.getOperand() + ")";
		}
		
		private static final long serialVersionUID = -4183372597787862911L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Sqrt extends UnaryOperation.Abstract {
		
		public Sqrt(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return Math.sqrt(this.getOperand().getAsDouble());
		}
		
		@Override
		public final String toString() {
			return "(sqrt " + this.getOperand() + ")";
		}
		
		private static final long serialVersionUID = 5812533032365148428L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Inverse extends UnaryOperation.Abstract {
		
		public Inverse(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return 1.0 / this.getOperand().getAsDouble();
		}
		
		@Override
		public final String toString() {
			return "(1/" + this.getOperand() + ")";
		}
		
		private static final long serialVersionUID = -4183372597787862911L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Sum extends NaryOperation.Abstract {
		
		@Override
		public final String getOperator() {
			return "+";
		}
		
		@Override
		public final double getAsDouble() {
			return this.getOperands().stream().mapToDouble(DoubleSupplier::getAsDouble).sum();
		}
		
		private static final long serialVersionUID = -505389182598778793L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Product extends NaryOperation.Abstract {
		
		@Override
		public final String getOperator() {
			return " ";
		}
		
		@Override
		public final double getAsDouble() {
			return this.getOperands().stream().mapToDouble(DoubleSupplier::getAsDouble).reduce(1.0, Product::product);
		}
		
		private static final long serialVersionUID = -505389182598778793L;
		
		public static final double product(final double a, final double b) {
			return a * b;
		}
		
	}
	
}
