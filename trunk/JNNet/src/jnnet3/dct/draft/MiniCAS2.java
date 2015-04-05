package jnnet3.dct.draft;

import static java.util.Arrays.stream;
import static net.sourceforge.aprog.tools.Tools.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleSupplier;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-05)
 */
public final class MiniCAS2 {
	
	private MiniCAS2() {
		throw new IllegalInstantiationException();
	}
	
	public static final Value ZERO = new Value(0.0);
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final Variable i = new Variable("i");
		final Sum sum = new Sum(i, i, new Value(10.0));
		
		debugPrint(sum, "=", sum.getAsDouble());
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
		
		private final double[] data;
		
		private final Variable index;
		
		public Value(final double... data) {
			this.data = data;
			this.index = new Variable("");
		}
		
		public final Variable getIndex() {
			return this.index;
		}
		
		public final int getElementCount() {
			return this.data.length;
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		@Override
		public final double getAsDouble() {
			return this.data[(int) this.getIndex().getAsDouble()];
		}
		
		@Override
		public final String toString() {
			return join(", ", stream(this.data).mapToObj(Double::toString).toArray());
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
		public static abstract class Abstract implements Iteration {
			
			private final List<Expression> operands;
			
			protected Abstract(final Expression expression, final Variable variable, final Expression count) {
				this.operands = Arrays.asList(expression, variable, count);
			}
			
			@Override
			public final List<Expression> getOperands() {
				return this.operands;
			}
			
			@Override
			public final String toString() {
				return this.protectedToString();
			}
			
			protected String protectedToString() {
				return "(" + this.getExpression() + ")_" + this.getVariable() + "<" + this.getCount();
			}
			
			private static final long serialVersionUID = 56516015189794650L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-05)
	 */
	public static final class Sequence extends Iteration.Abstract {
		
		public Sequence(final Value value) {
			super(value, value.getIndex(), new Value(value.getElementCount()));
		}
		
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
			return "Π" + super.protectedToString();
		}
		
		private static final long serialVersionUID = 1115752578667847713L;
		
	}
	
}
