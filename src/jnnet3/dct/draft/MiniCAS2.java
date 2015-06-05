package jnnet3.dct.draft;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.toList;
import static multij.tools.Tools.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-05)
 */
public final class MiniCAS2 {
	
	private MiniCAS2() {
		throw new IllegalInstantiationException();
	}
	
	public static final Value ZERO = new Value(0.0);
	
	public static final Value ONE = new Value(1.0);
	
	public static final Value MINUS_ONE = new Value(-1.0);
	
	public static final Value PI = new Value(Math.PI);
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final Variable i = variable("i");
		final Sum sum = sum(i, i, 10);
		final Product product = product(add(i, 1), i, 10);
		
		debugPrint(sum, "=", sum.getAsDouble());
		debugPrint(product, "=", product.getAsDouble());
		debugPrint(add(2, 2), "=", add(2, 2).getAsDouble());
		debugPrint(sum(multiply("a", cos("x")), i, "n"));
	}
	
	public static final Value v(final double value) {
		return new Value(value);
	}
	
	public static final Variable variable(final String name) {
		return new Variable(name);
	}
	
	public static final Sum sum(final Concatenation concatenation) {
		return sum(concatenation, concatenation.getIndex(), v(concatenation.getOperands().size()));
	}
	
	public static final Sum sum(final Object expression, final Object variable, final Object count) {
		return new Sum(expression(expression), (Variable) expression(variable), expression(count));
	}
	
	public static final Sum add(final Object... objects) {
		return sum(concatenate(objects));
	}
	
	public static final Product product(final Concatenation concatenation) {
		return product(concatenation, concatenation.getIndex(), v(concatenation.getOperands().size()));
	}
	
	public static final Product product(final Object expression, final Object variable, final Object count) {
		return new Product(expression(expression), (Variable) expression(variable), expression(count));
	}
	
	public static final Product multiply(final Object... objects) {
		return product(concatenate(objects));
	}
	
	public static final Sum subtract(final Object left, final Object right) {
		return add(left, multiply(MINUS_ONE, right));
	}
	
	public static final Product divide(final Object left, final Object right) {
		return multiply(left, invert(right));
	}
	
	public static final Concatenation concatenate(final Object... objects) {
		final Concatenation result = new Concatenation();
		
		result.getOperands().addAll(stream(objects).map(MiniCAS2::expression).collect(toList()));
		
		return result;
	}
	
	public static final Expression expression(final Object... objects) {
		if (objects.length == 1) {
			final Object object = objects[0];
			
			return object instanceof Expression ? (Expression) object :
				object instanceof String ? variable(object.toString()) :
					object instanceof Number ? v(((Number) object).doubleValue()) : null;
		}
		
		return concatenate(objects);
	}
	
	public static final Inverse invert(final Object operand) {
		return new Inverse(expression(operand));
	}
	
	public static final Cos cos(final Object operand) {
		return new Cos(expression(operand));
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static abstract interface Expression extends Serializable {
		
		public default <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		public abstract double getAsDouble();
		
		/**
		 * @author codistmonk (creation 2015-04-05)
		 */
		public static abstract interface Visitor<V> extends Serializable, DoubleSupplier {
			
			public default V visit(final Expression expression) {
				return null;
			}
			
			public default V visit(final Value value) {
				return this.visit((Expression) value);
			}
			
			public default V visit(final Variable variable) {
				return this.visit((Expression) variable);
			}
			
			public default V visit(final Operation operation) {
				return this.visit((Expression) operation);
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static final class Value implements Expression {
		
		private final double value;
		
		public Value(final double value) {
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
		public final String toString() {
			final long longValue = (long) this.getAsDouble();
			
			return longValue == this.getAsDouble() ? Long.toString(longValue) :
				String.format(Locale.ENGLISH, "%.3f", this.getAsDouble());
		}
		
		private static final long serialVersionUID = -7428349794913779700L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static final class Variable implements Expression {
		
		private final String name;
		
		private Expression value;
		
		public Variable(final String name) {
			this.name = name;
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		public final Expression getValue() {
			return this.value;
		}
		
		public final void setValue(final Expression value) {
			this.value = value;
		}
		
		@Override
		public final double getAsDouble() {
			return this.getValue() == null ? 0.0 : this.getValue().getAsDouble();
		}
		
		@Override
		public final String toString() {
			return this.name;
		}
		
		private static final long serialVersionUID = -7206153614728380589L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static abstract interface Operation extends Expression {
		
		@Override
		public default <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		public abstract List<Expression> getOperands();
		
		/**
		 * @author codistmonk (creation 2015-04-06)
		 */
		public static abstract class Abstract implements Operation {
			
			private final List<Expression> operands;
			
			protected Abstract(final Expression... operands) {
				this(asList(operands));
			}
			
			protected Abstract(final List<Expression> operands) {
				this.operands = operands;
			}
			
			@Override
			public final List<Expression> getOperands() {
				return this.operands;
			}
			
			private static final long serialVersionUID = 4769691796254099201L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-06)
	 */
	public static final class Concatenation extends Operation.Abstract {
		
		private final Variable index;
		
		public Concatenation() {
			super(new ArrayList<>());
			this.index = new Variable("");
		}
		
		public final Variable getIndex() {
			return this.index;
		}
		
		@Override
		public final double getAsDouble() {
			return this.getOperands().get((int) this.getIndex().getAsDouble()).getAsDouble();
		}
		
		@Override
		public final String toString() {
			return join(", ", this.getOperands());
		}
		
		private static final long serialVersionUID = 8611209860000779554L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static abstract interface Iteration extends Operation {
		
		public default Expression getExpression() {
			return this.getOperands().get(0);
		}
		
		public default Variable getVariable() {
			return (Variable) this.getOperands().get(1);
		}
		
		public default Expression getCount() {
			return this.getOperands().get(2);
		}
		
		/**
		 * @author codistmonk (creation 2015-04-06)
		 */
		public static abstract class Abstract extends Operation.Abstract implements Iteration {
			
			protected Abstract(final Expression expression, final Variable variable, final Expression count) {
				super(expression, variable, count);
			}
			
			@Override
			public final String toString() {
				return this.protectedToString();
			}
			
			protected String protectedToString() {
				final StringBuilder resultBuilder = new StringBuilder();
				final Expression expression = this.getExpression();
				final boolean addPrentheses = expression instanceof Value || expression instanceof Variable;
				
				if (addPrentheses) {
					resultBuilder.append("(");
				}
				
				resultBuilder.append(expression);
				
				if (addPrentheses) {
					resultBuilder.append(")");
				}
				
//				resultBuilder.append("_0≤").append(this.getVariable()).append("<").append(this.getCount());
				resultBuilder.append("_").append(this.getVariable()).append("<").append(this.getCount());
				
				return resultBuilder.toString();
			}
			
			private static final long serialVersionUID = 56516015189794650L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static final class Sequence extends Iteration.Abstract {
		
		public Sequence(final Expression expression, final Variable variable,
				final Expression count) {
			super(expression, variable, count);
		}
		
		@Override
		public final double getAsDouble() {
			return this.getExpression().getAsDouble();
		}
		
		private static final long serialVersionUID = 4814506613325010396L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static final class Sum extends Iteration.Abstract {
		
		public Sum(final Expression expression, final Variable variable,
				final Expression count) {
			super(expression, variable, count);
		}
		
		@Override
		public final double getAsDouble() {
			final int n = (int) this.getCount().getAsDouble();
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				this.getVariable().setValue(new Value(i));
				result += this.getExpression().getAsDouble();
			}
			
			return result;
		}
		
		@Override
		protected final String protectedToString() {
			final Concatenation concatenationOperand = cast(Concatenation.class, this.getExpression());
			
			if (concatenationOperand != null && concatenationOperand.getIndex() == this.getVariable()) {
				return "(" + join("+", concatenationOperand.getOperands()) + ")";
			}
			
			return "Σ" + super.protectedToString();
		}
		
		private static final long serialVersionUID = 5641548238846722438L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static final class Product extends Iteration.Abstract {
		
		public Product(final Expression expression, final Variable variable,
				final Expression count) {
			super(expression, variable, count);
		}
		
		@Override
		public final double getAsDouble() {
			final int n = (int) this.getCount().getAsDouble();
			double result = 1.0;
			
			for (int i = 0; i < n; ++i) {
				this.getVariable().setValue(new Value(i));
				result *= this.getExpression().getAsDouble();
			}
			
			return result;
		}
		
		@Override
		protected final String protectedToString() {
			final Concatenation concatenationOperand = cast(Concatenation.class, this.getExpression());
			
			if (concatenationOperand != null && concatenationOperand.getIndex() == this.getVariable()) {
				return "(" + join(" ", concatenationOperand.getOperands()) + ")";
			}
			
			return "Π" + super.protectedToString();
		}
		
		private static final long serialVersionUID = 1115752578667847713L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-06)
	 */
	public static final class Inverse extends Operation.Abstract {
		
		public Inverse(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return 1.0 / this.getOperands().get(0).getAsDouble();
		}
		
		@Override
		public final String toString() {
			return "(1/" + this.getOperands().get(0) + ")";
		}
		
		private static final long serialVersionUID = 5281325055444988205L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-06)
	 */
	public static final class Cos extends Operation.Abstract {
		
		public Cos(final Expression operand) {
			super(operand);
		}
		
		@Override
		public final double getAsDouble() {
			return Math.cos(this.getOperands().get(0).getAsDouble());
		}
		
		@Override
		public final String toString() {
			return "(cos " + this.getOperands().get(0) + ")";
		}
		
		private static final long serialVersionUID = 2502032154950506484L;
		
	}
	
}
