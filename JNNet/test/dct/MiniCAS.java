package dct;

import static java.lang.Math.min;
import static java.util.Collections.sort;
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
		return new Constant(((Number) value).doubleValue());
	}
	
	public static final Expression expression(final Object expression) {
		return expression instanceof Expression ? (Expression) expression :
			expression instanceof CharSequence ? variable(expression) : constant(expression);
	}
	
	public static final Expression sqrt(final Object expression) {
		return new Sqrt(expression(expression));
	}
	
	public static final Expression cos(final Object operand) {
		return new Cos(expression(operand));
	}
	
	public static final Expression negate(final Object operand) {
		return multiply(MINUS_ONE, operand);
	}
	
	public static final Expression invert(final Object operand) {
		return new Inverse(expression(operand));
	}
	
	public static final Expression add(final Object... operands) {
		final Sum result = new Sum();
		
		result.getOperands().addAll(Arrays.stream(operands).map(MiniCAS::expression).collect(toList()));
		
		return result;
	}
	
	public static final Expression multiply(final Object... operands) {
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
	
	/**
	 * @author codistmonk (creation 2015-03-29)
	 */
	public static final class Canonicalize implements Expression.Visitor<Expression> {
		
		@Override
		public final Expression visit(final Variable variable) {
			return variable;
		}
		
		@Override
		public final Expression visit(final Constant constant) {
			return constant;
		}
		
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
			final List<Expression> canonicalOperands = operands.stream().map(operand -> operand.accept(this)).collect(toList());
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
			
			if (!sameElements(operands, flattened)) {
				return operation.newInstance(flattened);
			}
			
			return operation;
		}
		
		private static final long serialVersionUID = 1135894630542113318L;
		
		public static final Canonicalize INSTANCE = new Canonicalize();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-28)
	 */
	public static abstract interface Expression extends Serializable, DoubleSupplier, Comparable<Expression> {
		
		public abstract <V> V accept(Visitor<V> visitor);
		
		@Override
		public default int compareTo(final Expression other) {
			return this.getType().compareTo(other.getType());
		}
		
		public default String getType() {
			return this.getClass().getName();
		}
		
		/**
		 * @author codistmonk (creation 2015-03-28)
		 * @param <V> 
		 */
		public static abstract interface Visitor<V> extends Serializable {
			
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
		public final String toString() {
			return String.format(Locale.ENGLISH, "%.2f", this.getAsDouble());
		}
		
		private static final long serialVersionUID = 4701039521481142899L;
		
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
			return "cos(" + this.getOperand() + ")";
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
			return "sqrt(" + this.getOperand() + ")";
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
