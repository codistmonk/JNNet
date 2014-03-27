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
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-25)
 */
public final class LinearConstraintSystemTest {
	
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
		
		system.addConstraint(1.0, -1.0, -1.0);
		
		assertTrue(system.accept(system.solve()));
		
		system.addConstraint(-1.0, -1.0, -1.0);
		
		assertFalse(system.accept(system.solve()));
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
	
	@Test
	public final void test4() {
		final LinearConstraintSystem system = new LinearConstraintSystem(4);
		
		system.addConstraint(0.0, 1.0, 0.0, 0.0);
		system.addConstraint(0.0, 0.0, 1.0, 0.0);
		system.addConstraint(0.0, 0.0, 0.0, 1.0);
		system.addConstraint(-6.0, 1.0, 2.0, 3.0);
		system.addConstraint(-5.0, 0.0, 0.0, 1.0);
		
		final double[] solution = system.solve();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test5() {
		final LinearConstraintSystem system = Tools.readObject("test/jnnet4/mnist0_system.jo");
		
		debugPrint(system.getData().size(), system.getOrder());
		
		final double[] solution = system.solve();
		
		debugPrint(Arrays.toString(solution));
		assertTrue(system.accept(solution));
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
				final double value = this.evaluate(i, point);
				
				if (value + EPSILON < 0.0) {
					debugPrint(i, value);
					
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
			final int extraDimension = order;
			final double[] extendedData = new double[data.length / order * extendedOrder];
			
			for (int i = 0, j = 0; i < data.length; i += order, j += extendedOrder) {
				final double vectorNorm = vectorNorm(data, i, order);
				
				for (int kI = i, kJ = j; kI < i + order; ++kI, ++kJ) {
					extendedData[kJ] = data[kI] / vectorNorm;
				}
				
				extendedData[j + extraDimension] = -1.0;
			}
			
			final double[] extendedPoint = new double[extendedOrder];
			
			extendedPoint[0] = 1.0;
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value < 0.0) {
					extendedPoint[extraDimension] -= value / extendedData[i + extendedOrder - 1];
//					final double w = extendedData[i + extraDimension];
//					
//					for (int j = 0; j < extendedData.length; j += extendedOrder) {
//						for (int k = j + 1; k < j + extendedOrder; ++k) {
//							extendedData[k] *= w;
//						}
//					}
//					
//					for (int j = 0; j < extendedOrder; ++j) {
//						extendedPoint[j] *= w;
//					}
//					
//					extendedPoint[extraDimension] -= value;
				}
			}
			
//			for (int i = 0; i < extendedData.length; i += extendedOrder) {
//				debugPrint(i / extendedOrder, evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint));
//			}
			
			{
				final TicToc timer = new TicToc();
				int remainingIterations = 10000;
				
				timer.tic();
				
				while (this.updateExtendedPoint(extendedPoint, extendedData) && 0 <= --remainingIterations) {
					if (10000L <= timer.toc()) {
						debugPrint("remainingIterations:", remainingIterations);
						timer.tic();
					}
				}
				
				debugPrint("remainingIterations:", remainingIterations);
				debugPrint("extendedPoint:", Arrays.toString(extendedPoint));
			}
			
			return copyOf(extendedPoint, order);
		}

		private final boolean updateExtendedPoint(final double[] extendedPoint, final double[] extendedData) {
			final int order = this.getOrder();
			final int extendedOrder = extendedPoint.length;
			
			checkSolution(extendedData, extendedOrder, extendedPoint);
			
			final IntList limitIds = new IntList();
			final double[] extendedDirection = new double[extendedOrder];
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value <= 10.0 * EPSILON) {
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
			
			if (EPSILON < smallestTipValue) {
				extendedDirection[extendedOrder - 1] = smallestTipValue;
				
				double smallestDisplacement = Double.POSITIVE_INFINITY;
//				double debugSmallestAcceptedValue = Double.POSITIVE_INFINITY;
//				double debugExtendedDirectionValue = 0.0;
				
				for (int i = 0; i < extendedData.length; i += extendedOrder) {
					final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
					
					// (point + k * direction) . h = 0
					// value + k * direction . h = 0
					// k = - value / (direction . h)
					final double extendedDirectionValue = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedDirection);
					
					if (EPSILON < -extendedDirectionValue) {
						smallestDisplacement = min(smallestDisplacement, -value / extendedDirectionValue);
//						if (value < debugSmallestAcceptedValue) {
//							debugSmallestAcceptedValue = value;
//							debugExtendedDirectionValue = extendedDirectionValue;
//						}
//						debugPrint(value, extendedDirectionValue, i / extendedOrder, limitIds);
//					} else {
//						debugPrint(value, extendedDirectionValue, i / extendedOrder, limitIds);
					}
				}
				
				checkSolution(extendedData, extendedOrder, extendedPoint);
				
//				debugPrint(debugSmallestAcceptedValue, debugExtendedDirectionValue, limitIds);
				
				if (!Double.isInfinite(smallestDisplacement) && EPSILON < smallestDisplacement) {
//					debugPrint(smallestDisplacement);
					
					for (int i = 1; i < extendedOrder; ++i) {
						extendedPoint[i] += smallestDisplacement * extendedDirection[i];
					}
					
					checkSolution(extendedData, extendedOrder, extendedPoint);
					
					return true;
				}
				
//				debugPrint(smallestDisplacement);
			}
			
//			debugPrint(limitIds.size(), smallestTipValue, Arrays.toString(extendedPoint));
			
			return false;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 7450111830241146851L;
		
		/**
		 * {@value}.
		 */
		public static final double EPSILON = 1E-7;
		
		public static final void checkSolution(final double[] data, final int order, final double[] point) {
			for (int i = 0; i < data.length; i += order) {
				final double value = evaluate(data, order, i / order, point);
				
				if (value + EPSILON < 0.0) {
					debugPrint(i, i / order, value);
					throw new IllegalStateException();
				}
			}
		}
		
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
