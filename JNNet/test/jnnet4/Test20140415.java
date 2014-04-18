package jnnet4;

import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.util.Arrays.copyOfRange;
import static jnnet4.JNNetTools.irange;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jnnet.IntList;
import jnnet4.LinearConstraintSystemTest.LinearConstraintSystem;
import jnnet4.SortingTools.IndexComparator;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-04-12)
 */
public final class Test20140415 {
	
	private Test20140415() {
		throw new IllegalInstantiationException();
	}
	
	static final boolean debug = true;
	
	static final List<Point> path = new ArrayList<Point>();
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		{
			LinearConstraintSystem20140418.test();
		}
		
		{
			final double[] objective = { 0.0, 0.0, 0.0, 1.0 };
			final double[] constraints = {
					1.0, 6.0, 1.0, -1.0,
					2.0, 1.0, -7.0, -1.0,
			};
			
			debugPrint(eliminate(objective, constraints, irange(constraints.length / objective.length)));
			
			debugPrint("objective:", Arrays.toString(objective));
			
			for (int i = 0; i < constraints.length; i += objective.length) {
				final double d = dot(objective, 0, constraints, i, objective.length);
				
				if (d != 0.0) {
					debugPrint(d);
					throw new IllegalStateException();
				}
			}
		}
		
		{
			final int n = 5;
			final int k = 3;
			final int[] combination = irange(k);
			
			debugPrint(Arrays.toString(combination));
			
			while (nextCombination(combination, n)) {
				debugPrint(Arrays.toString(combination));
			}
		}
		
		new VisualConstraintBuilder();
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
	
	public static final void move(final double[] constraints, final double[] objective, final double[] solution) {
		final int n = constraints.length;
		
		if (n == 0) {
			return;
		}
		
		final int order = solution.length;
		double solutionValue = Double.NaN;
		double objectiveValue = Double.NaN;
		int offset = -1;
		boolean offsetIsUnsatisfiedCodirectionalConstraint = true;
		
		for (int i = 0; i < n; i += order) {
			final double value = dot(constraints, i, solution, 0, order);
			final double v = dot(constraints, i, objective, 0, order);
			
			if (0.0 == value && v < 0.0) {
				offset = -1;
				break;
			}
			
			// -value/v < -solutionValue/objectiveValue 
			// <- d < 0.0
			final double d = solutionValue * abs(v) * signum(objectiveValue) - value * abs(objectiveValue) * signum(v);
			
			if (0.0 < value && v < 0.0 && (d < 0.0 || isNaN(d))) {
				solutionValue = value;
				objectiveValue = v;
				offset = i;
				offsetIsUnsatisfiedCodirectionalConstraint = false;
			} else if (value < 0.0 && 0.0 < v && offsetIsUnsatisfiedCodirectionalConstraint && (0.0 < d || isNaN(d))) {
				solutionValue = value;
				objectiveValue = v;
				offset = i;
			}
		}
		
		if (0 <= offset) {
			// (solution + k * objective) . constraint = 0
			// <- solution . constraint + k * objective . constraint = 0
			// <- k = - value / objectiveValue
			add(abs(objectiveValue), solution, 0, -signum(objectiveValue) * solutionValue, objective, 0, solution, 0, order);
			
			if (debug) {
				path.add(point(solution));
			}
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
			debugPrint(i, "/", limits.length);
			
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
			
			while (nextCombination(combination, limits.length) && timer.toc() < 1L) {
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

	public static boolean objectiveIsCompatibleWithSelectedConstraints(final double[] objective,
			final double[] constraints, final int[] ids) {
		final int order = objective.length;
		
		for (final int id : ids) {
			if (dot(constraints, id * order, objective, 0, order) < 0.0) {
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
	
	public static final void add(final double scale1, final double[] data1, final int offset1,
			final double scale2, final double[] data2, final int offset2,
			final double[] result, final int resultOffset, final int dimension) {
		for (int i = 0; i < dimension; ++i) {
			result[resultOffset + i] = scale1 * data1[offset1 + i] + scale2 * data2[offset2 + i];
		}
	}
	
	public static final double dot(final double[] data1, final int offset1, final double[] data2, final int offset2, final int dimension) {
		double result = 0.0;
		
		for (int i = 0; i < dimension; ++i) {
			result += data1[offset1 + i] * data2[offset2 + i];
		}
		
		return result;
	}
	
	public static final double dot(final double[] data1, final double[] data2, final int dimension) {
		return dot(data1, 0, data2, 0, dimension);
	}
	
	public static final double dot(final double[] data1, final double[] data2) {
		return dot(data1, data2, data1.length);
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
	
	public static final double[] maximize(final double[] constraints, final double[] objective) {
		final double[] result = new double[objective.length];
		
		if (findLaxSolution(constraints, result)) {
			// TODO
		}
		
		return result;
	}
	
	public static final boolean findLaxSolution(final double[] constraints, final double[] solution) {
		int status;
		
		do {
			status = improveSolution(constraints, solution);
		} while (status == MORE_PROCESSING_NEEDED);
		
		return status == ALL_CONSTRAINTS_OK;
	}
	
	public static final int improveSolution(final double[] constraints, final double[] solution) {
		final int constraintId = findUnsatisfiedConstraintId(constraints, solution);
		
		if (constraintId < 0) {
			return ALL_CONSTRAINTS_OK;
		}
		
		final int dimension = solution.length;
		final double[] objective = new double[dimension];
		
		System.arraycopy(constraints, constraintId * dimension + 1, objective, 1, dimension - 1);
		
		move(constraints, objective, solution);
		
		if (0.0 <= dot(constraints, constraintId * dimension, solution, 0, dimension)) {
			return MORE_PROCESSING_NEEDED;
		}
		
		do {
			if (!eliminate(objective, constraints, listLimits(constraints, solution)) || allZeros(objective)) {
				return SYSTEM_KO;
			}
			
			move(constraints, objective, solution);
		} while (dot(constraints, constraintId * dimension, solution, 0, dimension) < 0.0);
		
		return MORE_PROCESSING_NEEDED;
	}
	
	public static final boolean allZeros(final double... values) {
		for (final double value : values) {
			if (value != 0.0) {
				return false;
			}
		}
		
		return true;
	}
	
	public static final int[] listLimits(final double[] constraints, final double[] solution) {
		final int n = constraints.length;
		final int dimension = solution.length;
		final IntList result = new IntList();
		
		for (int i = 0, j = 0; i < n; i += dimension, ++j) {
			if (dot(constraints, i, solution, 0, dimension) == 0.0) {
				result.add(j);
			}
		}
		
		return result.toArray();
	}
	
	public static final int findUnsatisfiedConstraintId(final double[] constraints, final double[] point) {
		final int n = constraints.length;
		final int dimension = point.length;
		
		for (int i = 0, j = 0; i < n; i += dimension, ++j) {
			if (dot(constraints, i, point, 0, dimension) < 0.0) {
				return j;
			}
		}
		
		return -1;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-14)
	 */
	public static final class VisualConstraintBuilder extends MouseAdapter implements Serializable, Painter<SimpleImageView> {
		
		private final SimpleImageView imageView;
		
		private final LinearConstraintSystem20140418 system;
		
		private final double[] solution;
		
		private final double[] objective;
		
		private final List<Point> vertices;
		
		public VisualConstraintBuilder() {
			this.imageView = new SimpleImageView();
			this.system = new LinearConstraintSystem20140418(3);
			this.solution = new double[] { 1.0, 0.0, 0.0 };
			this.objective = new double[] { 0.0, 0.0, -1.0 };
			this.vertices = new ArrayList<Point>();
			
			this.imageView.getImageHolder().addMouseListener(this);
			this.imageView.getImageHolder().addMouseMotionListener(this);
			this.imageView.getImageHolder().addMouseWheelListener(this);
			
			this.imageView.getPainters().add(this);
			this.imageView.addComponentListener(new ComponentAdapter() {
				
				@Override
				public final void componentResized(final ComponentEvent event) {
					imageView.getBuffer().setFormat(imageView.getWidth(), imageView.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
					imageView.refreshBuffer();
					imageView.setImage(imageView.getBufferImage());
				}
				
			});
			
			path.clear();
			
			show(this.imageView, Test20140415.class.getName(), false);
		}
		
		@Override
		public final void paint(final Graphics2D g, final SimpleImageView component, final int width, final int height) {
			this.imageView.getBuffer().clear(Color.BLACK);
			
			final int n = this.system.getConstraintCount();
			
			if (0 < n) {
				final int order = this.system.getOrder();
				final double[] constraints = this.system.getConstraints();
				final BufferedImage buffer = this.imageView.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						int score = 0;
						
						for (int i = 0; i < constraints.length; i += order) {
							// 1 * constraint[0] + x * constraint[1] + y * constraint[2] = 0
							if (0.0 <= constraints[i + 0] + x * constraints[i + 1] + y * constraints[i + 2]) {
								++score;
							}
						}
						
						final int rgb = 0xFF000000 | (0x00010101 * (score * 255 / n));
						
						buffer.setRGB(x, y, rgb);
					}
				}
			}
			
			final Graphics2D bufferGraphics = this.imageView.getBufferGraphics();
			
			{
				bufferGraphics.setColor(Color.RED);
				
				for (int i = 0; i < path.size() - 1; ++i) {
					bufferGraphics.drawLine(path.get(i).x, path.get(i).y, path.get(i + 1).x, path.get(i + 1).y);
				}
				
				final Point point = point(this.solution);
				bufferGraphics.fillOval(point.x - 2, point.y - 2, 4, 4);
			}
			
			for (final Point vertex : this.vertices) {
				bufferGraphics.setColor(Color.GREEN);
				bufferGraphics.drawOval(vertex.x - 3, vertex.y - 3, 6, 6);
			}
		}
		
		@Override
		public final void mouseClicked(final MouseEvent event) {
			if (event.getButton() == MouseEvent.BUTTON1) {
				this.vertices.add(event.getPoint());
				
				if (this.vertices.size() == 2) {
					debugPrint();
					
					final Point p1 = this.vertices.remove(0);
					final Point p0 = this.vertices.remove(0);
					
					this.system.addConstraint(
							det(p0.x, p0.y, p1.x, p1.y),
							det(1,    p1.y, 1,    p0.y),
							det(1,    p0.x, 1,    p1.x));
					
					for (int i = 0; i < this.system.getConstraintCount(); ++i) {
						System.out.println("	system.addConstraint(" +
								Arrays.toString(this.system.getConstraint(i)).replaceAll("\\[|\\]", "") + ");");
					}
					
					debugPrint(this.system.accept(this.system.solve(this.solution)));
				}
				
				this.imageView.refreshBuffer();
			} else if (event.getButton() == MouseEvent.BUTTON3) {
				this.solution[0] = 1.0;
				this.solution[1] = event.getX();
				this.solution[2] = event.getY();
				
				path.clear();
				path.add(point(this.solution));
				
				System.out.println("	system.solve(" +
						Arrays.toString(this.solution).replaceAll("\\[|\\]", "") + ");");
				
				debugPrint(this.system.accept(this.system.solve(this.solution)));
				
				this.imageView.refreshBuffer();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3122077265997975111L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-18)
	 */
	public static final class LinearConstraintSystem20140418 extends LinearConstraintSystem.Abstract {
		
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
		
		static final void test() {
			final LinearConstraintSystem20140418 system = new LinearConstraintSystem20140418(3);
			system.addConstraint(8446.0, -12.0, -98.0);
			system.addConstraint(13861.0, -5.0, -109.0);
			system.addConstraint(14841.0, -8.0, -73.0);
			debugPrint(system.accept(system.solve(1.0, 154.0, 208.0)));
		}
		
	}
	
}
