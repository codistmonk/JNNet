package jnnet4;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;

import jnnet.DoubleList;
import jnnet.IntList;
import net.sourceforge.aprog.tools.MathTools;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-25)
 */
public final class LinearConstraintSolverTest {
	
	@Test
	public final void test1() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		system.addConstraint(0.0, 1.0, 0.0);
		system.addConstraint(0.0, 0.0, 1.0);
		system.addConstraint(2.0, -1.0, -1.0);
		
		assertTrue(system.accept(1.0, 0.0, 0.0));
		assertTrue(system.accept(1.0, 1.0, 0.0));
		assertTrue(system.accept(1.0, 0.0, 1.0));
		assertTrue(system.accept(1.0, 0.5, 0.5));
		assertTrue(system.accept(1.0, 1.0, 1.0));
		
		assertFalse(system.accept(1.0, 1.5, 1.5));
		assertFalse(system.accept(1.0, -0.5, -0.5));
		
		assertTrue(system.accept(system.solve()));
	}
	
	@Test
	public final void test2() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		system.addConstraint(0.0, 1.0, 0.0);
		system.addConstraint(1.0, -1.0, 0.0);
		
		assertTrue(system.accept(1.0, 0.0, 0.0));
		assertTrue(system.accept(1.0, 1.0, 0.0));
		
		assertFalse(system.accept(1.0, 1.5, 0.0));
		assertFalse(system.accept(1.0, -0.5, 0.0));
		
		assertTrue(system.accept(system.solve()));
	}
	
	@Test
	public final void test3() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		system.addConstraint(0.0, -1.0, 0.0);
		system.addConstraint(-1.0, 1.0, 0.0);
		
		assertFalse(system.accept(1.0, 0.0, 0.0));
		assertFalse(system.accept(1.0, 1.0, 0.0));
		
		assertFalse(system.accept(1.0, 1.5, 0.0));
		assertFalse(system.accept(1.0, -0.5, 0.0));
		
		assertFalse(system.accept(system.solve()));
	}
	
	/**
	 * @author codistmonk (creation 2014-03-25)
	 */
	public static final class LinearConstraintSystem implements Serializable {
		
		private final DoubleList data;
		
		private final int order;
		
		public LinearConstraintSystem(final int order) {
			this.data = new DoubleList();
			this.order = order;
		}
		
		public final DoubleList getData() {
			return this.data;
		}
		
		public final int getOrder() {
			return this.order;
		}
		
		public final LinearConstraintSystem addConstraint(final double... constraint) {
			this.getData().addAll(constraint);
			
			return this;
		}
		
		public final boolean accept(final double... point) {
			final double[] data = this.getData().toArray();
			final int n = data.length / this.getOrder();
			
			for (int i = 0; i < n; ++i) {
				if (this.evaluate(i, point) < 0.0) {
					return false;
				}
			}
			
			return true;
		}
		
		public final double evaluate(final int constraintIndex, final double... point) {
			return evaluate(this.getData().toArray(),  this.getOrder(), constraintIndex, point);
		}
		
		public final double[] solve() {
			final double[] data = this.getData().toArray();
			final int order = this.getOrder();
			final int extendedOrder = order + 1;
			final double[] extendedData = new double[data.length / order * extendedOrder];
			
			for (int i = 0, j = 0; i < data.length; i += order, j += extendedOrder) {
				final double vectorNorm = vectorNorm(data, i, order);
				
				for (int kI = i, kJ = j; kI < i + order; ++kI, ++kJ) {
					extendedData[kJ] = data[kI] / vectorNorm;
				}
				
				extendedData[j + extendedOrder - 1] = -1.0;
			}
			
			debugPrint(Arrays.toString(extendedData));
			
			final double[] extendedPoint = new double[extendedOrder];
			
			extendedPoint[0] = 1.0;
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value < 0.0) {
					extendedPoint[extendedOrder - 1] -= value / extendedData[i + extendedOrder - 1];
				}
			}
			
			debugPrint("extendedPoint:", Arrays.toString(extendedPoint));
			
			{
				int remainingIterations = 10000;
				
				while (this.updateExtendedPoint(extendedPoint, extendedData) && 0 <= --remainingIterations);
			}
			
			debugPrint("extendedPoint:", Arrays.toString(extendedPoint));
			
			return copyOf(extendedPoint, order);
		}

		private final boolean updateExtendedPoint(final double[] extendedPoint, final double[] extendedData) {
			final int order = this.getOrder();
			final int extendedOrder = extendedPoint.length;
			final double epsilon = 1.0E-15;
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value + epsilon < 0.0) {
					debugPrint(i, i / extendedOrder, value);
					throw new IllegalStateException();
				}
			}
			
			final IntList limitIds = new IntList();
			final double[] extendedDirection = new double[extendedOrder];
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value + epsilon <= 2.0 * epsilon) {
					limitIds.add(i / extendedOrder);
					
					for (int j = i + 1; j < i + order; ++j) {
						extendedDirection[j - i] += extendedData[j];
					}
				}
			}
			
			double smallestTipValue = Double.POSITIVE_INFINITY;
			
			for (final int i : limitIds.toArray()) {
				smallestTipValue = min(smallestTipValue, evaluate(extendedData, extendedOrder, i, extendedDirection));
			}
			
			if (epsilon < smallestTipValue) {
				extendedDirection[extendedOrder - 1] = smallestTipValue;
				
				double smallestDisplacement = Double.POSITIVE_INFINITY;
				
				for (int i = 0; i < extendedData.length; i += extendedOrder) {
					final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
					
					if (epsilon < value) {
						// (point + k * direction) . h = 0
						// value + k * direction . h = 0
						// k = - value / (direction . h)
						final double extendedDirectionValue = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedDirection);
						
						smallestDisplacement = min(smallestDisplacement, -value / extendedDirectionValue);
					}
				}
				
				if (!Double.isInfinite(smallestDisplacement)) {
					for (int i = 1; i < extendedOrder; ++i) {
						extendedPoint[i] += smallestDisplacement * extendedDirection[i];
					}
					
					return true;
				}
			}
			
			return false;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 7450111830241146851L;
		
		public static final double vectorNorm(final double[] data, final int offset, final int order) {
			double sumOfSquares = 0.0;
			final int end = offset + order;
			
			for (int i = offset + 1; i < end; ++i) {
				sumOfSquares += square(data[i]);
			}
			
			return sqrt(sumOfSquares);
		}
		
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
	
}
