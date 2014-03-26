package jnnet4;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;
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
	public final void test() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		system.addConstraint(1.0, 0.0, 0.0);
		system.addConstraint(0.0, 1.0, 0.0);
		system.addConstraint(-1.0, -1.0, 2.0);
		
		assertTrue(system.accept(0.0, 0.0, 1.0));
		assertTrue(system.accept(1.0, 0.0, 1.0));
		assertTrue(system.accept(0.0, 1.0, 1.0));
		assertTrue(system.accept(0.5, 0.5, 1.0));
		assertTrue(system.accept(1.0, 1.0, 1.0));
		
		assertFalse(system.accept(1.5, 1.5, 1.0));
		assertFalse(system.accept(-0.5, -0.5, 1.0));
		
		assertTrue(system.accept(system.solve()));
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
//				System.arraycopy(data, i, extendedData, j, order);
//				extendedData[j + extendedOrder - 1] = -vectorNorm;
				
				for (int kI = i, kJ = j; kI < i + order; ++kI, ++kJ) {
					extendedData[kJ] = data[kI] / vectorNorm;
				}
				
				extendedData[j + extendedOrder - 1] = -1.0;
			}
			
			debugPrint(Arrays.toString(extendedData));
			
			final double[] extendedPoint = new double[extendedOrder];
			
			extendedPoint[extendedOrder - 2] = 1.0;
			
			debugPrint(Arrays.toString(extendedPoint));
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value < 0.0) {
					extendedPoint[extendedOrder - 1] -= value / extendedData[i + extendedOrder - 1];
					debugPrint(i, value, Arrays.toString(extendedPoint));
				}
			}
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value < 0.0) {
					debugPrint(i, value);
					throw new IllegalStateException();
				}
			}
			
			debugPrint(Arrays.toString(extendedPoint));
			
			final IntList limitIds = new IntList();
			final double[] direction = new double[extendedOrder];
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (0.0 == value) {
					limitIds.add(i / extendedOrder);
					
					for (int j = i; j < i + order; ++j) {
						direction[j - i] += extendedData[j];
					}
				}
			}
			
			double smallestTipValue = Double.POSITIVE_INFINITY;
			
			for (final int i : limitIds.toArray()) {
				smallestTipValue = min(smallestTipValue, evaluate(extendedData, extendedOrder, i, direction));
			}
			
			if (smallestTipValue <= 0.0) {
				debugPrint("Optimum reached");
			} else {
				direction[extendedOrder - 1] = smallestTipValue;
				debugPrint(Arrays.toString(direction));
			}
			
			return null;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 7450111830241146851L;
		
		public static final double vectorNorm(final double[] data, final int offset, final int order) {
			double sumOfSquares = 0.0;
			final int end = offset + order - 1;
			
			for (int i = offset; i < end; ++i) {
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
