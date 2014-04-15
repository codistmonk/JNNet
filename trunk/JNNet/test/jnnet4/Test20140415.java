package jnnet4;

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
import java.util.Collection;
import java.util.List;

import jnnet.DoubleList;
import jnnet.IntList;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-12)
 */
public final class Test20140415 {
	
	private Test20140415() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		{
			final double[] objective = { 0.0, 0.0, 0.0, 1.0 };
			final double[] constraints = {
					1.0, 6.0, 1.0, -1.0,
					2.0, 1.0, -7.0, -1.0,
			};
			
			debugPrint(eliminate(objective, constraints, irange(constraints.length / objective.length)));
			
			debugPrint("objective:", Arrays.toString(objective));
			
			for (int i = 0; i < constraints.length; i += objective.length) {
				debugPrint(dot(objective, 0, constraints, i, objective.length));
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
	
	/**
	 * @author codistmonk (creation 2014-04-14)
	 */
	public static final class VisualConstraintBuilder extends MouseAdapter implements Serializable, Painter<SimpleImageView> {
		
		private final SimpleImageView imageView;
		
		private final DoubleList constraints;
		
		private final int order;
		
		private final double[] solution;
		
		private final double[] objective;
		
		private final List<Point> vertices;
		
		private final List<Point> path;
		
		public VisualConstraintBuilder() {
			this.imageView = new SimpleImageView();
			this.constraints = new DoubleList();
			this.order = 3;
			this.solution = new double[] { 1.0, 0.0, 0.0 };
			this.objective = new double[] { 0.0, 0.0, -1.0 };
			this.vertices = new ArrayList<>();
			this.path = new ArrayList<>();
			
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
			
			show(this.imageView, Test20140415.class.getName(), false);
		}
		
		@Override
		public final void paint(final Graphics2D g, final SimpleImageView component, final int width, final int height) {
			this.imageView.getBuffer().clear(Color.BLACK);
			
			final int n = this.constraints.size() / this.order;
			
			if (0 < n) {
				final BufferedImage buffer = this.imageView.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						int score = 0;
						
						for (int i = 0; i < this.constraints.size(); i += this.order) {
							// 1 * constraint[0] + x * constraint[1] + y * constraint[2] = 0
							if (0.0 <= constraints.get(i + 0) + x * constraints.get(i + 1) + y * constraints.get(i + 2)) {
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
				
				for (int i = 0; i < this.path.size() - 1; ++i) {
					bufferGraphics.drawLine(this.path.get(i).x, this.path.get(i).y, this.path.get(i + 1).x, this.path.get(i + 1).y);
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
			if (event.getClickCount() == 2) {
				this.vertices.add(event.getPoint());
				
				if (this.vertices.size() == 2) {
					debugPrint();
					
					final Point p1 = this.vertices.remove(0);
					final Point p0 = this.vertices.remove(0);
					
					this.constraints.add(det(p0.x, p0.y, p1.x, p1.y));
					this.constraints.add(det(1, p1.y, 1, p0.y));
					this.constraints.add(det(1, p0.x, 1, p1.x));
					
					for (int i = 0; i < this.constraints.size(); i += this.order) {
						System.out.println("	constraints.addAll(" +
								Arrays.toString(copyOfRange(this.constraints.toArray(), i, i + this.order)).replaceAll("\\[|\\]", "") + ");");
					}
					
					move(this.constraints.toArray()/*, this.objective*/, this.solution, this.path);
				}
				
				this.imageView.refreshBuffer();
			} else if (event.getButton() == MouseEvent.BUTTON3) {
				this.solution[0] = 1.0;
				this.solution[1] = event.getX();
				this.solution[2] = event.getY();
				
				this.path.clear();
				this.path.add(point(this.solution));
				debugPrint(move(this.constraints.toArray()/*, this.objective*/, this.solution, this.path));
				debugPrint(move(this.constraints.toArray()/*, this.objective*/, this.solution, this.path));
				
				this.imageView.refreshBuffer();
			}
		}
		
		public static final IntList move(final double[] constraints, final double[] objective, final double[] solution) {
			final IntList result = new IntList();
			final int n = constraints.length;
			final int order = solution.length;
			
			double solutionValue = Double.NEGATIVE_INFINITY;
			double objectiveValue = 0.0;
			int offset = -1;
			
			for (int i = 0; i < n; i += order) {
				final double v = dot(constraints, i, objective, 0, order);
				
				if (v < 0.0) {
					final double value = dot(constraints, i, solution, 0, order);
					
					if (0.0 == value) {
						result.add(i);
					} else if (result.isEmpty() && 0.0 < value &&
							(solutionValue * v - value * objectiveValue) * (v < 0.0 ? -1.0 : 1.0) * (objectiveValue < 0.0 ? -1.0 : 1.0) < 0) {
						solutionValue = value;
						objectiveValue = v;
						offset = i;
					}
				}
			}
			
			if (0 <= offset) {
				// (solution + k * objective) . constraint = 0
				// <- solution . constraint + k * objective . constraint = 0
				// <- k = - value / objectiveValue
				add(objectiveValue, solution, 0, -solutionValue, objective, 0, solution, 0, order);
			}
			
			return result;
		}
		
		public static final IntList move(final double[] constraints, final double[] solution, final Collection<Point> path) {
			final IntList result = new IntList();
			final int n = constraints.length;
			final int order = solution.length;
			final double[] objective = new double[order];
			double solutionValue = Double.NEGATIVE_INFINITY;
			double objectiveValue = 0.0;
			int offset = -1;
			
			for (int i = 0; i < n; i += order) {
				final double value = dot(constraints, i, solution, 0, order);
				
				if (value < 0.0) {
					System.arraycopy(constraints, i + 1, objective, 1, order - 1);
					
					final double v = dot(constraints, i, objective, 0, order);
					
					if ((solutionValue * v - value * objectiveValue) * (v < 0.0 ? -1.0 : 1.0) * (objectiveValue < 0.0 ? -1.0 : 1.0) < 0.0) {
						solutionValue = value;
						objectiveValue = v;
						offset = i;
					}
				}
			}
			
			if (0 <= offset) {
				System.arraycopy(constraints, offset + 1, objective, 1, order - 1);
				
				{
					for (int i = 0; i < n; i += order) {
						final double value = dot(constraints, i, solution, 0, order);
						
						if (value == 0.0) {
							final double v = dot(constraints, i, objective, 0, order);
							
							if (v < 0.0) {
								result.add(i);
							}
						}
					}
				}
				
				if (result.isEmpty()) {
					// (solution + k * objective) . constraint = 0
					// <- solution . constraint + k * objective . constraint = 0
					// <- k = - value / objectiveValue
					add(objectiveValue, solution, 0, -solutionValue, objective, 0, solution, 0, order);
					path.add(point(solution));
				}
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3122077265997975111L;
		
		public static final Point point(final double[] wxy) {
			return new Point((int) round(wxy[1] / wxy[0]), (int) round(wxy[2] / wxy[0]));
		}
		
	}
	
	public static final int det(final int a, final int b, final int c, final int d) {
		return a * d - b * c;
	}
	
	public static final IntList ilist(final int... values) {
		return new IntList(values.length).addAll(values);
	}
	
	public static final boolean eliminate(final double[] objective, final double[] constraints, final int... limits) {
		for (int i = 0; i < limits.length; ++i) {
			final int[] combination = irange(i);
			final int[] ids = new int[i];
			
			for (int j = 0; j < i; ++j) {
				ids[j] = limits[combination[j]];
			}
			
			debugPrint(Arrays.toString(ids));
			
			if (eliminate(objective, constraints, ids, limits)) {
				return true;
			}
			
			while (nextCombination(combination, limits.length)) {
				for (int j = 0; j < i; ++j) {
					ids[j] = limits[combination[j]];
				}
				
				debugPrint(Arrays.toString(ids));
				
				if (eliminate(objective, constraints, ids, limits)) {
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
				debugPrint(offset);
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
	
}
