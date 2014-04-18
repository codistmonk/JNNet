package jnnet4;

import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static jnnet4.LinearConstraintSystem20140418.ALL_CONSTRAINTS_OK;
import static jnnet4.LinearConstraintSystem20140418.MORE_PROCESSING_NEEDED;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-04-18)
 */
public final class LinearConstraintSystemBigDecimal implements LinearConstraintSystem {
	
	private final int order;
	
	private final ArrayList<BigDecimal> constraints;
	
	public LinearConstraintSystemBigDecimal(final int order) {
		this.order = order;
		this.constraints = new ArrayList<BigDecimal>();
	}
	
	@Override
	public final int getOrder() {
		return this.order;
	}
	
	@Override
	public final LinearConstraintSystemBigDecimal allocate(final int constraintCount) {
		this.constraints.ensureCapacity(constraintCount * this.getOrder());
		
		return this;
	}

	@Override
	public final LinearConstraintSystemBigDecimal addConstraint(final double... constraint) {
		this.allocate(this.getConstraintCount() + 1);
		
		for (final double value : constraint) {
			this.constraints.add(BigDecimal.valueOf(value));
		}
		
		return this;
	}
	
	@Override
	public final int getConstraintCount() {
		return this.constraints.size() / this.getOrder();
	}
	
	@Override
	public final double[] getConstraint(final int constraintId) {
		final int n = this.getOrder();
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.constraints.get(constraintId * n + i).doubleValue();
		}
		
		return result;
	}
	
	@Override
	public final double[] getConstraints() {
		final int n = this.constraints.size();
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.constraints.get(i).doubleValue();
		}
		
		return result;
	}
	
	@Override
	public final boolean accept(final double... point) {
		return isSolution(this.constraints, v(point));
	}
	
	@Override
	public final double[] solve() {
		final int order = this.getOrder();
		final double[] result = new double[order];
		result[0] = 1.0;
		final List<BigDecimal> solution = v(result);
		
		findLaxSolution(this.constraints, solution);
		
		for (int i = 0; i < order; ++i) {
			result[i] = solution.get(i).doubleValue();
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8427605502062887372L;
	
	public static final boolean findLaxSolution(final List<BigDecimal> constraints, final List<BigDecimal> solution) {
		int remaining = 1000;
		int status;
		
		do {
			status = improveSolution(constraints, solution);
		} while (status == MORE_PROCESSING_NEEDED && 0 < --remaining);
		
		return status == ALL_CONSTRAINTS_OK;
	}
	
	public static final int improveSolution(final List<BigDecimal> constraints, final List<BigDecimal> solution) {
		final int constraintId = findUnsatisfiedConstraintId(constraints, solution);
		
		if (constraintId < 0) {
			return ALL_CONSTRAINTS_OK;
		}
		
		final int dimension = solution.size();
		final List<BigDecimal> objective = extractObjective(constraints, constraintId * dimension, dimension);
		
		if (move(constraints, objective, solution) &&
				!isNegative(dot(constraints, constraintId * dimension, solution, 0, dimension))) {
			return MORE_PROCESSING_NEEDED;
		}
		
		// TODO
		
		return MORE_PROCESSING_NEEDED;
	}
	
	public static final boolean move(final List<BigDecimal> constraints, final List<BigDecimal> objective,
			final List<BigDecimal> solution) {
		final int n = constraints.size();
		final int order = solution.size();
		BigDecimal selectedSolutionValue = BigDecimal.ZERO;
		BigDecimal selectedObjectiveValue = BigDecimal.ZERO;
		int offset = -1;
		boolean offsetIsUnsatisfiedCodirectionalConstraint = true;
		
		for (int i = 0; i < n; i += order) {
			final BigDecimal solutionValue = dot(constraints, i, solution, 0, order);
			final BigDecimal objectiveValue = dot(constraints, i, objective, 0, order);
			
//			debugPrint(i, value, v, offsetIsUnsatisfiedCodirectionalConstraint);
			
			if (isZero(solutionValue) && isNegative(objectiveValue)) {
				return false;
			}
			
			// -value/v < -solutionValue/objectiveValue 
			// <- d < 0.0
			final BigDecimal d = selectedSolutionValue.multiply(objectiveValue.abs()).multiply(signum(selectedObjectiveValue))
					.subtract(solutionValue.multiply(selectedObjectiveValue.abs()).multiply(signum(objectiveValue)));
			
			if (isPositive(solutionValue) && isNegative(objectiveValue) && (offset < 0 || isNegative(d))) {
				selectedSolutionValue = solutionValue;
				selectedObjectiveValue = objectiveValue;
				offset = i;
				offsetIsUnsatisfiedCodirectionalConstraint = false;
			} else if (isNegative(solutionValue) && isPositive(objectiveValue) && offsetIsUnsatisfiedCodirectionalConstraint &&
					(offset < 0 || isPositive(d))) {
				selectedSolutionValue = solutionValue;
				selectedObjectiveValue = objectiveValue;
				offset = i;
			}
		}
		
		if (offset < 0) {
			return false;
		}
		
		// (solution + k * objective) . constraint = 0
		// <- solution . constraint + k * objective . constraint = 0
		// <- k = - value / objectiveValue
		add(selectedObjectiveValue.abs(), solution, 0,
				signum(selectedObjectiveValue).negate().multiply(selectedSolutionValue), objective, 0,
				solution, 0, order);
		
		return true;
	}
	
	public static final void add(final BigDecimal scale1, final List<BigDecimal> data1, final int offset1,
			final BigDecimal scale2, final List<BigDecimal> data2, final int offset2,
			final List<BigDecimal> result, final int resultOffset, final int dimension) {
		for (int i = 0; i < dimension; ++i) {
			result.set(resultOffset + i,
					scale1.multiply(data1.get(offset1 + i)).add(scale2.multiply(data2.get(offset2 + i))));
		}
	}
	
	public static final BigDecimal signum(final BigDecimal value) {
		return isNegative(value) ? BigDecimal.valueOf(-1.0) : BigDecimal.ONE;
	}
	
	public static final List<BigDecimal> extractObjective(final List<BigDecimal> data, final int offset, final int order) {
		final List<BigDecimal> result = new ArrayList<BigDecimal>(order);
		
		result.add(BigDecimal.ZERO);
		
		for (int i = 1; i < order; ++i) {
			result.add(data.get(offset + i));
		}
		
		return result;
	}
	
	public static final int findUnsatisfiedConstraintId(final List<BigDecimal> constraints, final List<BigDecimal> point) {
		final int n = constraints.size();
		final int dimension = point.size();
		
		for (int offset = 0, id = 0; offset < n; offset += dimension, ++id) {
			if (isNegative(dot(constraints, offset, point, 0, dimension))) {
				return id;
			}
		}
		
		return -1;
	}
	
	public static final boolean isSolution(final List<BigDecimal> constraints, final List<BigDecimal> point) {
		return findUnsatisfiedConstraintId(constraints, point) < 0;
	}
	
	public static final BigDecimal dot(final List<BigDecimal> data1, final int offset1, final List<BigDecimal> data2, final int offset2, final int dimension) {
		BigDecimal result = new BigDecimal(0.0);
		
		for (int i = 0; i < dimension; ++i) {
			result = result.add(data1.get(offset1 + i).multiply(data2.get(offset2 + i)));
		}
		
		return result;
	}
	
	public static final List<BigDecimal> v(final double... values) {
		final List<BigDecimal> result = new ArrayList<BigDecimal>(values.length);
		
		for (final double value : values) {
			result.add(BigDecimal.valueOf(value));
		}
		
		return result;
	}
	
	public static final boolean isZero(final BigDecimal value) {
		return value.compareTo(BigDecimal.ZERO) == 0;
	}
	
	public static final boolean isNegative(final BigDecimal value) {
		return value.compareTo(BigDecimal.ZERO) < 0;
	}
	
	public static final boolean isPositive(final BigDecimal value) {
		return 0 < value.compareTo(BigDecimal.ZERO);
	}
	
}
