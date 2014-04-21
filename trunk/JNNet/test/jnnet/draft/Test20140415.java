package jnnet.draft;

import static jnnet.JNNetTools.irange;
import static jnnet.draft.LinearConstraintSystem.Abstract.unscale;
import static jnnet.draft.LinearConstraintSystem20140418.det;
import static jnnet.draft.LinearConstraintSystem20140418.dot;
import static jnnet.draft.LinearConstraintSystem20140418.eliminate;
import static jnnet.draft.LinearConstraintSystem20140418.maximize;
import static jnnet.draft.LinearConstraintSystem20140418.nextCombination;
import static jnnet.draft.LinearConstraintSystem20140418.path;
import static jnnet.draft.LinearConstraintSystem20140418.point;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
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

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

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
	
	/**
	 * @author codistmonk (creation 2014-04-14)
	 */
	public static final class VisualConstraintBuilder extends MouseAdapter implements Serializable, Painter<SimpleImageView> {
		
		private final SimpleImageView imageView;
		
		private final LinearConstraintSystem20140419 system;
		
		private final double[] solution;
		
		private final double[] objective;
		
		private final List<Point> vertices;
		
		public VisualConstraintBuilder() {
			this.imageView = new SimpleImageView();
			this.system = new LinearConstraintSystem20140419(3);
			this.solution = new double[] { 1.0, 0.0, 0.0 };
			this.objective = new double[] { 0.0, 0.0, -1.0 };
			this.vertices = new ArrayList<Point>();
			
			this.system.addConstraint(1.0, 0.0, 0.0);
			this.system.addConstraint(0.0, 1.0, 0.0);
			this.system.addConstraint(0.0, 0.0, 1.0);
			
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
			
			LinearConstraintSystem20140418.debug = true;
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
					
					if (this.system.accept(this.system.solve(this.solution))) {
						debugPrint("Solution found:", Arrays.toString(unscale(this.solution)));
						maximize(this.system.getConstraints(), this.objective, this.solution);
						debugPrint("Optimum:", Arrays.toString(unscale(this.solution)));
						
						if (!this.system.accept(this.solution)) {
							System.err.println(debug(DEBUG_STACK_OFFSET, "Solution destroyed"));
						}
					} else {
						System.err.println(debug(DEBUG_STACK_OFFSET, "No Solution found"));
					}
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
				
				if (this.system.accept(this.system.solve(this.solution))) {
					debugPrint("Solution found:", Arrays.toString(unscale(this.solution)));
					maximize(this.system.getConstraints(), this.objective, this.solution);
					debugPrint("Optimum:", Arrays.toString(unscale(this.solution)), Arrays.toString(this.objective));
					
					if (!this.system.accept(this.solution)) {
						System.err.println(debug(DEBUG_STACK_OFFSET, "Solution destroyed"));
					}
				} else {
					System.err.println(debug(DEBUG_STACK_OFFSET, "No Solution found"));
				}
				
				this.imageView.refreshBuffer();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3122077265997975111L;
		
	}
	
}
