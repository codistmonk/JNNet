package jnnet4;

import static java.lang.Double.isNaN;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import jnnet.DoubleList;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-05)
 */
public abstract interface LinearConstraintSystem extends Serializable {
	
	public abstract int getOrder();
	
	public abstract LinearConstraintSystem allocate(int constraintCount);
	
	public abstract LinearConstraintSystem addConstraint(double... constraint);
	
	public abstract int getConstraintCount();
	
	public abstract double[] getConstraint(int constraintId);
	
	public abstract double[] getConstraints();
	
	public abstract boolean accept(double... point);
	
	public abstract double[] solve();
	
	/**
	 * @author codistmonk (creation 2014-04-18)
	 */
	public static abstract class Abstract implements LinearConstraintSystem {
		
		private final int order;
		
		private final DoubleList data;
		
		protected Abstract(final int order) {
			this.order = order;
			this.data = new DoubleList();
		}
		
		@Override
		public final int getOrder() {
			return this.order;
		}
		
		@Override
		public final double[] getConstraints() {
			return this.getData().toArray();
		}
		
		@Override
		public final Abstract allocate(final int constraintCount) {
			final int n = this.getData().size();
			final int needed = n + constraintCount * this.getOrder();
			
			debugPrint("Allocating", needed, "doubles");
			
			this.getData().resize(needed).resize(n);
			
			return this;
		}
		
		@Override
		public final Abstract addConstraint(final double... constraint) {
			this.getData().addAll(constraint);
			
			return this;
		}
		
		@Override
		public final int getConstraintCount() {
			return this.getData().size() / this.getOrder();
		}
		
		@Override
		public final double[] getConstraint(final int constraintId) {
			final int order = this.getOrder();
			final double[] result = new double[order];
			
			System.arraycopy(this.getData().toArray(), constraintId * order, result, 0, order);
			
			return result;
		}
		
		@Override
		public final boolean accept(final double... point) {
			if (0.0 == point[0]) {
				return false;
			}
			
			final int n = this.getConstraintCount();
			
			for (int i = 0; i < n; ++i) {
				final double value = this.evaluate(i, point);
				
				if (isNaN(value) || value + EPSILON < 0.0) {
					debugPrint(i, value);
					
					return false;
				}
			}
			
			return true;
		}
		
		public final double evaluate(final int constraintId, final double... point) {
			return evaluate(this.getData().toArray(),  this.getOrder(), constraintId, point);
		}
		
		private final DoubleList getData() {
			return this.data;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -4102583885017172499L;
		
		/**
		 * {@value}.
		 */
		public static final double EPSILON = 1E-7;
		
		public static final double evaluate(final double[] data, final int order, final int constraintIndex, final double... point) {
			final int begin = constraintIndex * order;
			final int end = begin + order;
			double result = 0.0;
			
			for (int i = begin; i < end; ++i) {
				result += data[i] * point[i - begin];
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-05)
	 */
	public static final class IO {
		
		private IO() {
			throw new IllegalInstantiationException();
		}
		
		public static final <T extends LinearConstraintSystem> T read(final String inputId, final Class<T> resultClass, final boolean closeInput) {
			return read(new DataInputStream(getResourceAsStream(inputId)), resultClass, closeInput);
		}
		
		public static final <T extends LinearConstraintSystem> T read(final DataInputStream input, final Class<T> resultClass, final boolean closeInput) {
			try {
				final int order = input.readInt();
				int constraintCount = input.readInt();
				final double[] constraint = new double[order];
				final T result = resultClass.getConstructor(int.class).newInstance(order);
				
				while (0 <= --constraintCount) {
					for (int j = 0; j < order; ++j) {
						constraint[j] = input.readDouble();
					}
					
					result.addConstraint(constraint);
				}
				
				return result;
			} catch (final Exception exception) {
				throw unchecked(exception);
			} finally {
				if (closeInput) {
					try {
						input.close();
					} catch (final IOException exception) {
						throw unchecked(exception);
					}
				}
			}
		}
		
		public static final void write(final LinearConstraintSystem system, final String outputId, final boolean closeOutput) {
			try {
				write(system, new DataOutputStream(new FileOutputStream(outputId)), closeOutput);
			} catch (final FileNotFoundException exception) {
				throw unchecked(exception);
			}
		}
		
		public static final void write(final LinearConstraintSystem system, final DataOutputStream output, final boolean closeOutput) {
			try {
				final int order = system.getOrder();
				final int constraintCount = system.getConstraintCount();
				
				output.writeInt(order);
				output.writeInt(constraintCount);
				
				for (int constraintId = 0; constraintId < constraintCount; ++constraintId) {
					for (final double value : system.getConstraint(constraintId)) {
						output.writeDouble(value);
					}
				}
			} catch (final Exception exception) {
				throw unchecked(exception);
			} finally {
				if (closeOutput) {
					try {
						output.close();
					} catch (final IOException exception) {
						throw unchecked(exception);
					}
				}
			}
		}
		
	}
	
}
