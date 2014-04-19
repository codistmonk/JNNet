package jnnet4;

import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.util.Arrays;

/**
 * @author codistmonk (creation 2014-04-19)
 */
public final class LinearConstraintSystem20140419 extends LinearConstraintSystem.Abstract {
	
	public LinearConstraintSystem20140419(final int order) {
		super(order);
	}
	
	public final double[] solve(final double... writeOnlyResult) {
		System.arraycopy(this.solve(), 0, writeOnlyResult, 0, this.getOrder());
		
		return writeOnlyResult;
	}
	
	@Override
	public final double[] solve() {
		final int order = this.getOrder();
		final double[] constraints = this.getConstraints();
		final int n = constraints.length;
		
		for (int i = 0; i < n; i += order) {
			final double d = LinearConstraintSystem20140414.vectorNorm(constraints, i - 1, order + 1);
			
			if (0.0 < d) {
				add(0.0, constraints, i, 1.0 / d, constraints, i, constraints, i, order);
			}
		}
		
		final double[] result = this.getConstraint(0);
		
		for (int i = order; i < n; i += order) {
			final double constraintValue = dot(result, 0, constraints, i, order);
			double alpha = 0.0;
			
			if (constraintValue <= 0.0) {
				final double lowerBound = -constraintValue / dot(constraints, i, constraints, i, order);
				double upperBound = lowerBound + 2.0;
				
				for (int j = 0; j < i; j += order) {
					final double d = dot(constraints, i, constraints, j, order);
					
					if (d < 0.0) {
						upperBound = min(upperBound, -dot(result, 0, constraints, j, order) / d);
					}
				}
				
				if (upperBound <= lowerBound) {
					System.err.println(debug(DEBUG_STACK_OFFSET, lowerBound, upperBound));
				}
				
				alpha = (lowerBound + upperBound) / 2.0;
			}
			
			add(1.0, result, 0, alpha, constraints, i, result, 0, order);
			
			if (LinearConstraintSystem20140418.debug) {
				LinearConstraintSystem20140418.path.add(LinearConstraintSystem20140418.point(result));
			}
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1878315283094231026L;
	
}