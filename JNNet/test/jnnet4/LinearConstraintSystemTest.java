package jnnet4;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;
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
		
//		system.addConstraint(1.0, 0.0, 0.0);
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
		
		assertTrue(system.accept(system.solve2()));
		
		system.addConstraint(1.0, -1.0, -1.0);
		
		assertTrue(system.accept(system.solve2()));
		
		system.addConstraint(-1.0, -1.0, -1.0);
		
		assertFalse(system.accept(system.solve2()));
	}
	
	@Test
	public final void test2() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		
		system.addConstraint(1.0, 0.0, 0.0);
		system.addConstraint(0.0, 1.0, 0.0);
		system.addConstraint(1.0, -1.0, 0.0);
		
		assertTrue(system.accept(1.0, 0.0, 0.0));
		assertTrue(system.accept(1.0, 1.0, 0.0));
		
		assertFalse(system.accept(1.0, 1.5, 0.0));
		assertFalse(system.accept(1.0, -0.5, 0.0));
		
		assertTrue(system.accept(system.solve2()));
	}
	
	@Test
	public final void test3() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		
		system.addConstraint(1.0, 0.0, 0.0);
		system.addConstraint(0.0, -1.0, 0.0);
		system.addConstraint(-1.0, 1.0, 0.0);
		
		assertFalse(system.accept(1.0, 0.0, 0.0));
		assertFalse(system.accept(1.0, 1.0, 0.0));
		
		assertFalse(system.accept(1.0, 1.5, 0.0));
		assertFalse(system.accept(1.0, -0.5, 0.0));
		
		assertFalse(system.accept(system.solve2()));
	}
	
	@Test
	public final void test4() {
		final LinearConstraintSystem system = new LinearConstraintSystem(4);
		
		system.addConstraint(1.0, 0.0, 0.0, 0.0);
		system.addConstraint(0.0, 1.0, 0.0, 0.0);
		system.addConstraint(0.0, 0.0, 1.0, 0.0);
		system.addConstraint(0.0, 0.0, 0.0, 1.0);
		system.addConstraint(-6.0, 1.0, 2.0, 3.0);
		system.addConstraint(-5.0, 0.0, 0.0, 1.0);
		
		final double[] solution = system.solve2();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test5() {
		final LinearConstraintSystem system = new LinearConstraintSystem(3);
		final double k = 1.0 / 1000.0;
		
		system.addConstraint(1.0, 0.0, 0.0);
		system.addConstraint(0.0, 1.0, 0.0);
		// (k,0,1)x(1/2,1/2,1)
		debugPrint(Arrays.toString(cross(v(k, 0.0, 1.0), v(0.5, 0.5, 1.0))));
		system.addConstraint(cross(v(k, 0.0, 1.0), v(0.5, 0.5, 1.0)));
		// (1/2,1/2,1)x(0,k,1)
		debugPrint(Arrays.toString(cross(v(0.5, 0.5, 1.0), v(0.0, k, 1.0))));
		system.addConstraint(cross(v(0.5, 0.5, 1.0), v(0.0, k, 1.0)));
		
		assertTrue(system.accept(1.0, 1.0, 2.0));
		
		final double[] solution = system.solve2();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test6() {
		final LinearConstraintSystem system = Tools.readObject("test/jnnet4/mnist0_system.jo");
		
//		{
//			final double[] constraint = new double[system.getOrder()];
//			
//			constraint[0] = 1.0;
//			
//			system.addConstraint(constraint);
//		}
		
		debugPrint(system.getData().size(), system.getOrder());
		
		final double[] solution = system.solve2();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test7() {
		final LinearConstraintSystem system = Tools.readObject("test/jnnet4/mnist4_system.jo");
		
		debugPrint(system.getData().size(), system.getOrder());
		
		final double[] solution = system.solve2();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	/**
	 * {@value}.
	 */
	public static final int X = 0;
	
	/**
	 * {@value}.
	 */
	public static final int Y = 1;
	
	/**
	 * {@value}.
	 */
	public static final int Z = 2;
	
	public static final double[] v(final double... v) {
		return v;
	}
	
	public static final double[] cross(final double[] v1, final double[] v2) {
		return new double[] {
				det(v1[Y], v1[Z], v2[Y], v2[Z]),
				det(v1[Z], v1[X], v2[Z], v2[X]),
				det(v1[X], v1[Y], v2[X], v2[Y])
		};
	}
	
	public static final double det(final double a, final double b, final double c, final double d) {
		return a * d - b * c;
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
		
		public final double evaluate(final int constraintId, final double... point) {
			return evaluate(this.getData().toArray(),  this.getOrder(), constraintId, point);
		}
		
		public final double[] solve2() {
			final int algo = 1;
			final double[] data = this.getData().toArray();
			final int order = this.getOrder();
			final double[] result = copyOf(data, order);
			
			if (algo == 0) {
				return this.solve();
			} else if (algo == 1) {
				final TicToc timer = new TicToc();
				int remainingIterations = 1000000;
				boolean done;
				
				timer.tic();
				
				fill(result, 1.0);
				
				do {
					if (5000L <= timer.toc()) {
						debugPrint("remainingIterations:", remainingIterations);
						timer.tic();
					}
					
					done = true;
					
					for (int i = 0; i < data.length; i += order) {
						final double value = dot(result, 0, data, i, order);
						
						if (value <= 0.0) {
							done = false;
							final double k = -value / dot(data, i, data, i, order);
							
							for (int j = 0; j < order; ++j) {
								result[j] += k * data[i + j];
							}
							
//							debugPrint(i / order, value, k, dot(result, 0, data, i, order));
						}
					}
					
//					debugPrint(remainingIterations, done, Arrays.toString(result));
				} while (!done && 0 < --remainingIterations);
				
				debugPrint(remainingIterations);
				
				return unscale(result);
			}
			
			for (int i = order; i < data.length; i += order) {
//				debugPrint(Arrays.toString(result));
				double alpha = 0.0;
				final double denominatorI = dot(result, 0, data, i, order);
				
				if (0.0 < denominatorI) {
					alpha = -dot(data, i, data, i, order) / denominatorI;
				}
				
//				debugPrint(i / order, alpha, dot(data, i, data, i, order), denominatorI);
				
				for (int j = 0; j < i; j += order) {
					double denominatorJ = dot(result, 0, data, j, order);
					
					if (denominatorJ < 0.0) {
//						debugPrint(i / order, j / order, denominatorJ);
						denominatorJ = 0.0;
					}
					
					if (0.0 != denominatorJ) {
						final double ratio = -dot(data, i, data, j, order) / denominatorJ;
						
						if (alpha < ratio) {
							alpha = ratio;
							
//							debugPrint(i / order, j / order, alpha, dot(data, i, data, j, order), denominatorJ);
						}
					}
				}
				
				if (0.0 <= denominatorI) {
					alpha = alpha + 0.5;
				} else {
					double maxAlpha = -dot(data, i, data, i, order) / denominatorI;
					
					if (maxAlpha <= alpha) {
						debugPrint(i / order, alpha, maxAlpha);
//						throw new IllegalStateException();
					}
					
					alpha = (alpha + maxAlpha) / 2.0;
				}
				
				for (int j = 0; j < order; ++j) {
					result[j] = alpha * result[j] + data[i + j];
				}
				
//				debugPrint(alpha);
				
				for (int j = 0; j < i; j += order) {
					if (dot(result, 0, data, j, order) < 0.0) {
//						debugPrint(j / order, dot(result, 0, data, j, order), denominatorI, alpha);
						
//						throw new IllegalStateException();
					}
				}
			}
			
//			debugPrint(Arrays.toString(result));
			
			return result;
		}
		
		public static final double[] unscale(final double[] v) {
			final double scale = v[0];
			
			if (scale != 0.0) {
				final int n = v.length;
				
				for (int i = 0; i < n; ++i) {
					v[i] /= scale;
				}
			}
			
			return v;
		}
		
		public static final double dot(final double[] data1, final int offset1, final double[] data2, final int offset2, final int n) {
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				result += data1[offset1 + i] * data2[offset2 + i];
			}
			
			return result;
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
//			Arrays.fill(extendedPoint, 1.0);
			
//			debugPrint(Arrays.toString(Arrays.copyOfRange(extendedData, 1572 * extendedOrder, 1573 * extendedOrder)));
//			debugPrint(Arrays.toString(Arrays.copyOfRange(extendedData, 1576 * extendedOrder, 1577 * extendedOrder)));
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
				
				if (value < 0.0) {
					extendedPoint[extraDimension] -= value / extendedData[i + extendedOrder - 1];
				} else if (-EPSILON <= evaluate(data, order, i / extendedOrder, extendedPoint)) {
					extendedData[i + extraDimension] = 0.0;
				}
			}
			
			{
				final TicToc timer = new TicToc();
				int remainingIterations = 1000;
				
				timer.tic();
				
				while (extendedPoint[extraDimension] <= -EPSILON &&
						this.updateExtendedPoint(extendedPoint, extendedData) && 0 <= --remainingIterations) {
					if (5000L <= timer.toc()) {
						debugPrint("remainingIterations:", remainingIterations);
						debugPrint("extendedPoint[extraDimension]:", extendedPoint[extraDimension]);
						timer.tic();
					}
				}
				
				debugPrint("remainingIterations:", remainingIterations);
				debugPrint("extendedPoint[extraDimension]:", extendedPoint[extraDimension]);
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
				final int constraintId = i / extendedOrder;
				final double value = evaluate(extendedData, extendedOrder, constraintId, extendedPoint);
				
				// XXX Maybe more constraints should be used as limits to prevent them from capping the displacement computation
				if (value <= 1.0) {
					limitIds.add(constraintId);
					
					for (int j = i + 1; j < i + order; ++j) {
						extendedDirection[j - i] += extendedData[j];
					}
				}
			}
			
			// XXX if smallestTipValue is too large, the displacement will be smaller and the convergence may fail
			// XXX if smallestTipValue is too small, the convergence will be slower
			double smallestTipValue = 0.1;
			
			for (final int i : limitIds.toArray()) {
				smallestTipValue = min(smallestTipValue,
						abs(evaluate(extendedData, extendedOrder, i, extendedDirection) / extendedData[i * extendedOrder + extendedOrder - 1]));
			}
			
			if (EPSILON < smallestTipValue) {
				extendedDirection[extendedOrder - 1] += smallestTipValue;
				
				double smallestDisplacement = -extendedPoint[extendedOrder - 1] / smallestTipValue;
				
				for (int i = 0; i < extendedData.length; i += extendedOrder) {
					// (point + k * direction) . h = 0
					// value + k * direction . h = 0
					// k = - value / (direction . h)
					final double extendedDirectionValue = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedDirection);
					
					if (EPSILON < -extendedDirectionValue) {
						final double value = evaluate(extendedData, extendedOrder, i / extendedOrder, extendedPoint);
						smallestDisplacement = min(smallestDisplacement, -value / extendedDirectionValue);
					}
				}
				
				checkSolution(extendedData, extendedOrder, extendedPoint);
				
				if (!Double.isInfinite(smallestDisplacement) && EPSILON < smallestDisplacement) {
					for (int i = 1; i < extendedOrder; ++i) {
						extendedPoint[i] += smallestDisplacement * extendedDirection[i];
					}
					
					checkSolution(extendedData, extendedOrder, extendedPoint);
					
					return true;
				}
				
				debugPrint(smallestDisplacement);
			}
			
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
