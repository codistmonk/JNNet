package jnnet4;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOf;
import static jnnet4.LinearConstraintSystem.Abstract.dot;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.util.Arrays;

import jnnet.DoubleList;
import jnnet.IntList;
import net.sourceforge.aprog.tools.TicToc;
	
/**
 * @author codistmonk (creation 2014-04-14)
 */
public final class LinearConstraintSystem20140414 implements LinearConstraintSystem {
	
	private final DoubleList extendedData;
	
	private final int order;
	
	public LinearConstraintSystem20140414(final int order) {
		this.order = order;
		this.extendedData = new DoubleList();
	}
	
	@Override
	public final int getOrder() {
		return this.order;
	}
	
	@Override
	public final LinearConstraintSystem20140414 allocate(final int constraintCount) {
		final int n = this.extendedData.size();
		final int needed = n + constraintCount * (this.getOrder() + 1);
		
		debugPrint("Allocating", needed, "doubles");
		
		this.extendedData.resize(needed).resize(n);
		
		return this;
	}
	
	@Override
	public final LinearConstraintSystem20140414 addConstraint(final double... constraint) {
		final double vectorNorm = vectorNorm(constraint, 0, this.getOrder());
		
		for (int i = 0; i < this.getOrder(); ++i) {
			this.extendedData.add(constraint[i] / vectorNorm);
		}
		
		this.extendedData.add(-1.0);
		
		return this;
	}
	
	@Override
	public final int getConstraintCount() {
		return this.extendedData.size() / (this.getOrder() + 1);
	}
	
	@Override
	public final double[] getConstraint(final int constraintId) {
		final int order = this.getOrder();
		final double[] result = new double[order];
		
		System.arraycopy(this.extendedData.toArray(), constraintId * (order + 1), result, 0, order);
		
		return result;
	}
	
	@Override
	public final double[] getConstraints() {
		final DoubleList result = new DoubleList(this.getConstraintCount() * this.getOrder());
		
		for (int i = 0; i < this.getConstraintCount(); ++i) {
			result.addAll(this.getConstraint(i));
		}
		
		return result.toArray();
	}
	
	@Override
	public final boolean accept(final double... point) {
		final int n = this.getConstraintCount();
		
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
		return evaluate(this.extendedData.toArray(), this.getOrder() + 1, this.getOrder(), constraintId, point);
	}
	
	@Override
	public final double[] solve() {
		final int order = this.getOrder();
		final int extendedOrder = order + 1;
		final int extraDimension = order;
		final double[] extendedData = this.extendedData.toArray();
		final double[] extendedPoint = new double[extendedOrder];
		
		extendedPoint[0] = 1.0;
		
		for (int i = 0; i < extendedData.length; i += extendedOrder) {
			final double value = evaluate(extendedData, extendedOrder, extendedOrder, i / extendedOrder, extendedPoint);
			
			if (value < 0.0) {
				extendedPoint[extraDimension] -= value / extendedData[i + extendedOrder - 1];
			} else if (-EPSILON <= evaluate(extendedData, extendedOrder, order, i / extendedOrder, extendedPoint)) {
				extendedData[i + extraDimension] = 0.0;
			}
		}
		
		{
			final TicToc timer = new TicToc();
			int remainingIterations = 5000;
			
			timer.tic();
			
			while (extendedPoint[extraDimension] <= -EPSILON &&
					this.updateExtendedPoint(extendedPoint, extendedData) && 0 < remainingIterations--) {
				if (10000L <= timer.toc()) {
					debugPrint(Thread.currentThread(), "remainingIterations:", remainingIterations, "extendedPoint[extraDimension]:", extendedPoint[extraDimension]);
					timer.tic();
				}
			}
			
			debugPrint(Thread.currentThread(), "remainingIterations:", remainingIterations, "extendedPoint[extraDimension]:", extendedPoint[extraDimension]);
		}
		
		return copyOf(extendedPoint, order);
	}
	
	private final boolean updateExtendedPoint(final double[] extendedPoint, final double[] extendedData) {
		final int order = this.getOrder();
		final int extendedOrder = extendedPoint.length;
		
		checkSolution(extendedData, extendedPoint);
		
		final IntList limitIds = new IntList();
		final double[] extendedDirection = new double[extendedOrder];
		
		// XXX The main problem is computing the direction; it seems to be equivalent to solving the same problem with a reduced set of constraints
		for (int i = 0; i < extendedData.length; i += extendedOrder) {
			final int constraintId = i / extendedOrder;
			final double value = evaluate(extendedData, extendedOrder, extendedOrder, constraintId, extendedPoint);
			
			// XXX Maybe more constraints should be used as limits to prevent them from capping the displacement computation
			if (value <= 0.5) {
				limitIds.add(constraintId);
				
				for (int j = i + 1; j < i + order; ++j) {
					extendedDirection[j - i] += extendedData[j] * 0.5;
				}
			}
		}
		
		// XXX If smallestTipValue is too large, the displacement will be smaller and the convergence may fail
		// XXX If smallestTipValue is too small, the convergence will be slower
		double smallestTipValue = 0.1;
		
		for (final int i : limitIds.toArray()) {
			smallestTipValue = min(smallestTipValue,
					abs(evaluate(extendedData, extendedOrder, extendedOrder, i, extendedDirection) / extendedData[i * extendedOrder + extendedOrder - 1]));
		}
		
		if (EPSILON < smallestTipValue) {
			extendedDirection[extendedOrder - 1] += smallestTipValue;
			
			double smallestDisplacement = -extendedPoint[extendedOrder - 1] / smallestTipValue;
			
			for (int i = 0; i < extendedData.length; i += extendedOrder) {
				// (point + k * direction) . h = 0
				// value + k * direction . h = 0
				// k = - value / (direction . h)
				final double extendedDirectionValue = evaluate(extendedData, extendedOrder, extendedOrder, i / extendedOrder, extendedDirection);
				
				if (EPSILON < -extendedDirectionValue) {
					final double value = evaluate(extendedData, extendedOrder, extendedOrder, i / extendedOrder, extendedPoint);
					smallestDisplacement = min(smallestDisplacement, -value / extendedDirectionValue);
				}
			}
			
			checkSolution(extendedData, extendedPoint);
			
			if (!Double.isInfinite(smallestDisplacement) && EPSILON < smallestDisplacement) {
				for (int i = 1; i < extendedOrder; ++i) {
					extendedPoint[i] += smallestDisplacement * extendedDirection[i];
				}
				
				checkSolution(extendedData, extendedPoint);
				
				return true;
			}
			
			debugPrint(smallestDisplacement);
		}
		
		return false;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -6252605545514219342L;
	
	/**
	 * {@value}.
	 */
	public static final double EPSILON = 1E-8;
	
	public static final int solveUsingLastDimension(final double[] data, final double[] point) {
		int result = -1;
		final int n = data.length;
		final int order = point.length;
		final int last = order - 1;
		
		for (int i = 0; i < n; i += order) {
			final double value = dot(data, i, point, 0, order);
			
			if (value < 0.0) {
				// (point + k * (0 .. 0, 1)) . h = 0
				// <- value + k * h[last] = 0
				// <- k = -value / h[last]
				point[last] -= value / data[i + last];
				
				debugPrint(Arrays.toString(point), dot(data, i, point, 0, order));
				
				result = i;
			}
		}
		
		debugPrint("lowest:", result / order);
		
		return result;
	}
	
	public static final void moveAlongConstraintToLastDimension0(final double[] data, final int constraintOffset, final double[] point) {
		final int order = point.length;
		final int last = order - 1;
		final double[] objective = new double[order];
		
		objective[last] = 1.0;
		
		// direction . h = 0
		// (objective + k1 * h) . h = 0
		// h[last] + k1 * h . h = 0
		// k1 = -h[last] / h . h
		final double k1 = -data[constraintOffset + last] / dot(data, constraintOffset, data, constraintOffset, order);
		
		// (point')[last] = 0
		// (point + k2 * direction)[last] = 0
		// point[last] + k2 * direction[last] = 0
		// k2 = -point[last] / direction[last]
		// k2 = -point[last] / (objective + k1 * h)[last]
		// k2 = -point[last] / (1 + k1 * h[last])
		final double k2 = -point[last] / (1.0 + k1 * data[constraintOffset + last]);
		
//			debugPrint(Arrays.toString(unscale(point)));
//			debugPrint(constraintOffset / order);
		
		for (int i = 0; i < order; ++i) {
			point[i] += k2 * (objective[i] + k1 * data[constraintOffset + i]);
		}
		
//			debugPrint(Arrays.toString(unscale(point)));
		debugPrint(point[last], dot(data, constraintOffset, point, 0, order));
		debugPrint("satisfied:", constraintOffset / order);
	}
	
	public static final void checkSolution(final double[] data, final double[] point) {
		final int order = point.length;
		
		for (int i = 0; i < data.length; i += order) {
			final double value = evaluate(data, order, order, i / order, point);
			
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
	
	public static final double evaluate(final double[] data, final int step, final int order, final int constraintIndex, final double... point) {
		final int begin = constraintIndex * step;
		final int end = begin + order;
		double result = 0.0;
		
		for (int i = begin; i < end; ++i) {
			result += data[i] * point[i - begin];
		}
		
		return result;
	}
	
}
