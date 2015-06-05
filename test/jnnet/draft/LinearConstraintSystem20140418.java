package jnnet.draft;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.util.Arrays.copyOfRange;
import static jnnet.JNNetTools.irange;
import static multij.tools.Tools.DEBUG_STACK_OFFSET;
import static multij.tools.Tools.debug;
import static multij.tools.Tools.debugPrint;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import multij.primitivelists.IntList;

import jnnet.draft.SortingTools.IndexComparator;

import multij.tools.TicToc;

/**
 * @author codistmonk (creation 2014-04-18)
 */
public final class LinearConstraintSystem20140418 extends LinearConstraintSystem.Abstract {
	
	public LinearConstraintSystem20140418(final int order) {
		super(order);
	}
	
	@Override
	public final double[] solve() {
		final double[] solution = new double[this.getOrder()];
		
		solution[0] = 1.0;
		
		return this.solve(solution);
	}
	
	public final double[] solve(final double... solution) {
		findLaxSolution(this.getConstraints(), solution);
		
		return solution;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 7481388639595747533L;
	
	static boolean debug = true;
	
	static final List<Point> path = new ArrayList<Point>();
	
	static final void test() {
		if (true) {
			final LinearConstraintSystem20140418 system = new LinearConstraintSystem20140418(3);
			system.addConstraint(8446.0, -12.0, -98.0);
			system.addConstraint(13861.0, -5.0, -109.0);
			system.addConstraint(14841.0, -8.0, -73.0);
			
			if (!system.accept(system.solve(1.0, 154.0, 208.0))) {
				throw new IllegalStateException();
			}
		}
		if (true) {
			final LinearConstraintSystem20140418 system = new LinearConstraintSystem20140418(3);
			system.addConstraint(0.0, 1.0, 0.0);
			system.addConstraint(0.0, 0.0, 1.0);
			system.addConstraint(482.0, -16.0, 30.0);
			system.addConstraint(-5020.0, 15.0, 55.0);
			
			final double[] solution = system.solve(1.0, 48.0, 69.0);
			
			if (!system.accept(solution)) {
				throw new IllegalStateException();
			}
			
			maximize(system.getConstraints(), new double[] { 0.0, 0.0, -1.0 }, solution);
			
			if (!system.accept(solution)) {
				throw new IllegalStateException();
			}
		}
		if (true) {
			final LinearConstraintSystem20140418 system = new LinearConstraintSystem20140418(3);
			system.addConstraint(0.0, 1.0, 0.0);
			system.addConstraint(0.0, 0.0, 1.0);
			system.addConstraint(-9493.0, 35.0, 94.0);
			
			final double[] solution = unscale(system.solve(1.0, 57.0, 47.0));
			
			debugPrint("expected:", "[1.0, 67.71464069178015, 75.77646357220952]");
			debugPrint("actual:", Arrays.toString(solution));
			
			if (!system.accept(solution)) {
				throw new IllegalStateException();
			}
			
			maximize(system.getConstraints(), new double[] { 0.0, 0.0, -1.0 }, solution);
			
			debugPrint("optimimum:", Arrays.toString(unscale(solution)));
			
			if (!system.accept(solution)) {
				throw new IllegalStateException();
			}
			
			if (!isZero(solution[2])) {
				throw new IllegalStateException();
			}
		}
	}
	
	public static final boolean nextCombination(final int[] combination, final int n) {
		int i = combination.length - 1;
		
		while (0 <= i) {
			if (++combination[i] < n + 1 - (combination.length - i)) {
				break;
			}
			
			--i;
		}
		
		if (i < 0) {
			return false;
		}
		
		for (int j = i + 1; j < combination.length; ++j) {
			combination[j] = combination[j - 1] + 1;
		}
		
		return true;
	}
	
	public static final Point point(final double[] wxy) {
		return new Point((int) round(wxy[1] / wxy[0]), (int) round(wxy[2] / wxy[0]));
	}
	
	public static final boolean move(final double[] constraints, final double[] objective, final double[] solution) {
		final int n = constraints.length;
		
		if (n == 0) {
			return false;
		}
		
		final int order = solution.length;
		double solutionValue = Double.NaN;
		double objectiveValue = Double.NaN;
		int offset = -1;
		boolean offsetIsUnsatisfiedCodirectionalConstraint = true;
		
		for (int i = 0; i < n; i += order) {
			final double value = dot(constraints, i, solution, 0, order);
			final double v = dot(constraints, i, objective, 0, order);
			
//			debugPrint(i, value, v, offsetIsUnsatisfiedCodirectionalConstraint);
			
			if (isZero(value) && isNegative(v)) {
				offset = -1;
				break;
			}
			
			// -value/v < -solutionValue/objectiveValue 
			// <- d < 0.0
			final double d = solutionValue * abs(v) * signum(objectiveValue) - value * abs(objectiveValue) * signum(v);
			
			if (isPositive(value) && isNegative(v) && (isNegative(d) || isNaN(d))) {
				solutionValue = value;
				objectiveValue = v;
				offset = i;
				offsetIsUnsatisfiedCodirectionalConstraint = false;
			} else if (isNegative(value) && isPositive(v) && offsetIsUnsatisfiedCodirectionalConstraint &&
					(isPositive(d) || isNaN(d))) {
				solutionValue = value;
				objectiveValue = v;
				offset = i;
			}
		}
		
		if (offset < 0) {
			return false;
		}
		
		final boolean solutionOkBeforeUpdate = isSolution(constraints, solution);
		
		// (solution + k * objective) . constraint = 0
		// <- solution . constraint + k * objective . constraint = 0
		// <- k = - value / objectiveValue
		add(abs(objectiveValue), solution, 0, -signum(objectiveValue) * solutionValue, objective, 0, solution, 0, order);
		
		final boolean solutionOkBeforeCondensation = isSolution(constraints, solution);
		
		if (solutionOkBeforeUpdate && !solutionOkBeforeCondensation) {
			System.err.println(debug(DEBUG_STACK_OFFSET, "WARNING: Update destroyed solution"));
		}
		
		condense(solution);
		
		if (solutionOkBeforeCondensation && !isSolution(constraints, solution)) {
			System.err.println(debug(DEBUG_STACK_OFFSET, "WARNING: Condensation destroyed solution"));
		}
		
		if (debug) {
			path.add(point(solution));
		}
		
		return true;
	}
	
	public static final boolean isSolution(final double[] constraints, final double[] point) {
		final int n = constraints.length;
		final int dimension = point.length;
		
		for (int i = 0; i < n; i += dimension) {
			final double value = dot(constraints, i, point, 0, dimension);
			
			if (isNegative(value)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static final void condense(final double... values) {
		double largestMagnitude = 0.0;
		
		for (final double value : values) {
			final double magnitude = abs(value);
			
			if (largestMagnitude < magnitude) {
				largestMagnitude = magnitude;
			}
		}
		
		if (largestMagnitude * EPSILON < 1.0) {
			return;
		}
		
		final int n = values.length;
		
		for (int i = 0; i < n; ++i) {
			values[i] /= largestMagnitude;
		}
	}
	
	public static final double signum(final double value) {
		return value < 0.0 ? -1.0 : 1.0;
	}
	
	public static final int det(final int a, final int b, final int c, final int d) {
		return a * d - b * c;
	}
	
	public static final IntList ilist(final int... values) {
		return new IntList(values.length).addAll(values);
	}
	
	public static final boolean eliminate(final double[] objective, final double[] constraints, final int... limits) {
		final int dimension = objective.length;
		final double[] tmp = new double[dimension];
		
		SortingTools.sort(limits, new IndexComparator() {
			
			@Override
			public final int compare(final int index1, final int index2) {
				final double d01 = dot(objective, 0, constraints, index1 * dimension, dimension);
				final double d02 = dot(objective, 0, constraints, index2 * dimension, dimension);
				final double d11 = dot(constraints, index1 * dimension, constraints, index1 * dimension, dimension);
				final double d22 = dot(constraints, index2 * dimension, constraints, index2 * dimension, dimension);
				
				return Double.compare(d01 / d11, d02 / d22);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6277762603619224047L;
			
		});
		
		final TicToc timer = new TicToc();
		
		for (int i = 0; i <= limits.length; ++i) {
//			debugPrint(i, "/", limits.length);
			
			final int[] combination = irange(i);
			final int[] ids = new int[i];
			
			for (int j = 0; j < i; ++j) {
				ids[j] = limits[combination[j]];
			}
			
			System.arraycopy(objective, 0, tmp, 0, dimension);
			
			if (eliminate(tmp, constraints, ids, limits)) {
				System.arraycopy(tmp, 0, objective, 0, dimension);
				
				return true;
			}
			
			timer.tic();
			
			while (nextCombination(combination, limits.length) && timer.toc() < 2L) {
				for (int j = 0; j < i; ++j) {
					ids[j] = limits[combination[j]];
				}
				
				System.arraycopy(objective, 0, tmp, 0, dimension);
				
				if (eliminate(tmp, constraints, ids, limits)) {
					System.arraycopy(tmp, 0, objective, 0, dimension);
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static final boolean eliminate(final double[] objective, final double[] constraints, final int[] ids, final int[] limits) {
		final int order = objective.length;
		
		if (0 < ids.length) {
			final double[] constraint = copyOfRange(constraints, ids[0] * order, (ids[0] + 1) * order);
			
			for (int i = 1; i < ids.length; ++i) {
				final int offset = ids[i] * order;
				final double d = dot(constraint, 1, constraint, 1, order - 1);
				
				eliminate(d, constraint, objective, 0, objective);
				eliminate(d, constraint, constraints, offset, constraint);
			}
			
			eliminate(dot(constraint, 1, constraint, 1, order - 1), constraint, objective, 0, objective);
		}
		
		return objectiveIsCompatibleWithSelectedConstraints(objective, constraints, limits);
	}
	
	public static final boolean objectiveIsCompatibleWithSelectedConstraints(final double[] objective,
			final double[] constraints, final int[] ids) {
		final int order = objective.length;
		
		for (final int id : ids) {
			final double d = dot(constraints, id * order, objective, 0, order);
			
			if (isNaN(d) || isNegative(d)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static final void eliminate(final double d, final double[] constraint,
			final double[] target, final int targetOffset,
			final double[] destination) {
		final int dimension = constraint.length;
		
		add(d, target, targetOffset + 1,
				-dot(target, targetOffset + 1, constraint, 1, dimension - 1), constraint, 1,
				destination, 1, dimension - 1);
	}
	
	/**
	 * {@value}.
	 */
	public static final int ALL_CONSTRAINTS_OK = 0;
	
	/**
	 * {@value}.
	 */
	public static final int MORE_PROCESSING_NEEDED = 1;
	
	/**
	 * {@value}.
	 */
	public static final int SYSTEM_KO = 2;
	
	public static final double[] maximize(final double[] constraints, final double[] objective, final double[] solution) {
		final boolean solutionOkBeforeOptimization = isSolution(constraints, solution);
		
		if (!solutionOkBeforeOptimization) {
			System.err.println(debug(DEBUG_STACK_OFFSET, "WARNING: Optimizing from invalid state"));
			
			return solution;
		}
		
		final int dimension = objective.length;
		final double[] tmp = objective.clone();
		
		while (eliminate(tmp, constraints, listLimits(constraints, solution)) &&
				!invalid(tmp) && move(constraints, tmp, solution)) {
			System.arraycopy(objective, 0, tmp, 0, dimension);
		}
		
		if (solutionOkBeforeOptimization && !isSolution(constraints, solution)) {
			System.err.println(debug(DEBUG_STACK_OFFSET, "WARNING: Optimization destroyed solution"));
		}
		
		return solution;
	}
	
	public static final boolean findLaxSolution(final double[] constraints, final double[] solution) {
		final TicToc timer = new TicToc();
		int status;
		
		timer.tic();
		
		do {
			status = improveSolution(constraints, solution, timer);
		} while (status == MORE_PROCESSING_NEEDED);
		
		return status == ALL_CONSTRAINTS_OK;
	}
	
	public static final int improveSolution(final double[] constraints, final double[] solution, final TicToc timer) {
		final int constraintId = findUnsatisfiedConstraintId(constraints, solution);
		
		if (constraintId < 0) {
			return ALL_CONSTRAINTS_OK;
		}
		
		final int dimension = solution.length;
		
		if (10000L <= timer.toc()) {
			System.out.println(constraintId + " / " + (constraints.length / solution.length));
			timer.tic();
		}
		
		final double[] objective = new double[dimension];
		
		System.arraycopy(constraints, constraintId * dimension + 1, objective, 1, dimension - 1);
		
		if (move(constraints, objective, solution) &&
				!isNegative(dot(constraints, constraintId * dimension, solution, 0, dimension))) {
			return MORE_PROCESSING_NEEDED;
		}
		
		do {
			System.arraycopy(constraints, constraintId * dimension + 1, objective, 1, dimension - 1);
			
			if (!eliminate(objective, constraints, listLimits(constraints, solution)) || invalid(objective)) {
				return SYSTEM_KO;
			}
		} while (move(constraints, objective, solution) &&
				isNegative(dot(constraints, constraintId * dimension, solution, 0, dimension)));
		
		return MORE_PROCESSING_NEEDED;
	}
	
	public static final boolean invalid(final double... values) {
		boolean result = true;
		
		for (final double value : values) {
			if (isNaN(value) || isInfinite(value)) {
				debugPrint(value);
				return true;
			}
			
			if (!isZero(value)) {
				result = false;
			}
		}
		
		return result;
	}
	
	public static final int[] listLimits(final double[] constraints, final double[] solution) {
		final int n = constraints.length;
		final int dimension = solution.length;
		final IntList result = new IntList();
		
		for (int i = 0, j = 0; i < n; i += dimension, ++j) {
			if (isZero(dot(constraints, i, solution, 0, dimension))) {
				result.add(j);
			}
		}
		
		return result.toArray();
	}
	
	public static final int findUnsatisfiedConstraintId(final double[] constraints, final double[] point) {
		final int n = constraints.length;
		final int dimension = point.length;
		
		for (int i = 0, j = 0; i < n; i += dimension, ++j) {
			if (isNegative(dot(constraints, i, point, 0, dimension))) {
				return j;
			}
		}
		
		return -1;
	}
	
}
