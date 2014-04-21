package jnnet.draft;

import static java.util.Arrays.copyOf;
import static jnnet.draft.LinearConstraintSystem.Abstract.unscale;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jnnet.DoubleList;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;

import org.junit.Test;
import org.ojalgo.optimisation.linear.CommonsMathSimplexSolverTest.GoalType;
import org.ojalgo.optimisation.linear.CommonsMathSimplexSolverTest.LinearConstraint;
import org.ojalgo.optimisation.linear.CommonsMathSimplexSolverTest.LinearObjectiveFunction;
import org.ojalgo.optimisation.linear.CommonsMathSimplexSolverTest.Relationship;
import org.ojalgo.optimisation.linear.CommonsMathSimplexSolverTest.SimplexSolver;

/**
 * @author codistmonk (creation 2014-03-25)
 */
public final class LinearConstraintSystemTest {
	
	@Test
	public final void test1() {
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 3).newInstance();
		
		system.addConstraint(1.0, 0.0, 0.0);
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
	public final void test1b() {
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 3).newInstance();
		
		system.addConstraint(1.0, 0.0, 0.0);
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
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 3).newInstance();
		
		system.addConstraint(1.0, 0.0, 0.0);
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
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 3).newInstance();
		
		system.addConstraint(1.0, 0.0, 0.0);
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
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 4).newInstance();
		
		system.addConstraint(1.0, 0.0, 0.0, 0.0);
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
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 3).newInstance();
		
		system.addConstraint(1.0, 0.0, 0.0);
		system.addConstraint(0.0, 1.0, 0.0);
		system.addConstraint(0.0, 0.0, 1.0);
		
		assertTrue(system.accept(1.0, 1.0, 1.0));
		
		final double[] solution = system.solve();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test6() {
		final LinearConstraintSystem system = DefaultFactory.forClass(CLASS, 3).newInstance();
		final double k = 1.0 / 1000.0;
		
		system.addConstraint(1.0, 0.0, 0.0);
		system.addConstraint(0.0, 1.0, 0.0);
		// (k,0,1)x(1/2,1/2,1)
		debugPrint(Arrays.toString(cross(v(k, 0.0, 1.0), v(0.5, 0.5, 1.0))));
		system.addConstraint(cross(v(k, 0.0, 1.0), v(0.5, 0.5, 1.0)));
		// (1/2,1/2,1)x(0,k,1)
		debugPrint(Arrays.toString(cross(v(0.5, 0.5, 1.0), v(0.0, k, 1.0))));
		system.addConstraint(cross(v(0.5, 0.5, 1.0), v(0.0, k, 1.0)));
		
		assertTrue(system.accept(0.5, 0.5, 2.0));
		
		final double[] solution = system.solve();
		
		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test7() {
		final LinearConstraintSystem system = LinearConstraintSystem.IO.read("test/jnnet4/mnist0_system.bin",
				CLASS, true);
//		debugPrint(system.getData().size(), system.getOrder());
		
		final double[] solution = system.solve();
		
//		debugPrint(Arrays.toString(solution));
		
		assertTrue(system.accept(solution));
	}
	
	@Test
	public final void test8() {
		final LinearConstraintSystem system = LinearConstraintSystem.IO.read("test/jnnet4/mnist4_system.bin",
				CLASS, true);
//		debugPrint(system.getData().size(), system.getOrder());
		
		final double[] solution = system.solve();
		
		debugPrint(Arrays.toString(unscale(solution)));
		
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
	
	public static final Class<? extends LinearConstraintSystem> CLASS = LinearConstraintSystem20140419.class;
	
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
	 * @author codistmonk (creation 2014-04-05)
	 */
	public static final class OjAlgoLinearConstraintSystem implements LinearConstraintSystem {
		
		private final LinearObjectiveFunction objective;
		
		private final List<LinearConstraint> constraints;
		
		public OjAlgoLinearConstraintSystem(final int order) {
			this.objective = new LinearObjectiveFunction(append(new double[order], 1.0), 0.0);
			this.constraints = new ArrayList<LinearConstraint>();
		}
		
		@Override
		public final int getOrder() {
			return this.objective.getVariables().size() - 1;
		}
		
		@Override
		public final LinearConstraintSystem allocate(final int constraintCount) {
			// TODO
			return this;
		}
		
		@Override
		public OjAlgoLinearConstraintSystem addConstraint(final double... constraint) {
			this.constraints.add(new LinearConstraint(append(constraint, -1.0), Relationship.GEQ, 1.0));
			
			return this;
		}
		
		@Override
		public final int getConstraintCount() {
			return this.constraints.size();
		}
		
		@Override
		public final double[] getConstraint(final int constraintId) {
			return copyOf(this.constraints.get(constraintId).getFactors(), this.getOrder());
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
			for (final LinearConstraint constraint : this.constraints) {
				if (LinearConstraintSystem.Abstract.dot(constraint.getFactors(), 0, point, 0, point.length) < 0.0) {
					return false;
				}
			}
			
			return true;
		}
		
		@Override
		public final double[] solve() {
			final SimplexSolver solver = new SimplexSolver();
			
			return copyOf(solver.optimize(this.objective, this.constraints, GoalType.MAXIMIZE, true).getPoint(), this.getOrder());
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -4869292727448695445L;
		
		public static final double[] append(final double[] array, final double value) {
			final int n = array.length;
			final double[] result = copyOf(array, n + 1);
			result[n] = value;
			
			return result;
		}
		
	}
	
}
