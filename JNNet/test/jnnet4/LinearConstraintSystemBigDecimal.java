package jnnet4;

import static jnnet4.LinearConstraintSystem20140418.ALL_CONSTRAINTS_OK;
import static jnnet4.LinearConstraintSystem20140418.MORE_PROCESSING_NEEDED;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
		final double[] result = new double[this.getOrder()];
		result[0] = 1.0;
		final List<BigDecimal> solution = v(result);
		
		// TODO
		
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
		
		// TODO
		
		return MORE_PROCESSING_NEEDED;
	}
	
	public static final int findUnsatisfiedConstraintId(final List<BigDecimal> constraints, final List<BigDecimal> point) {
		final int n = constraints.size();
		final int dimension = point.size();
		
		for (int i = 0, j = 0; i < n; i += dimension, ++j) {
			if (isNegative(dot(constraints, i, point, 0, dimension))) {
				return j;
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
