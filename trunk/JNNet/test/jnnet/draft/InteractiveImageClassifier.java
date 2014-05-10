package jnnet.draft;

import static imj2.tools.IMJTools.a8r8g8b8;
import static imj2.tools.IMJTools.alpha8;
import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static pixel3d.PolygonTools.X;
import static pixel3d.PolygonTools.Y;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

import pixel3d.MouseHandler;
import pixel3d.PolygonTools;
import pixel3d.PolygonTools.Processor;

/**
 * @author codistmonk (creation 2014-05-10)
 */
public final class InteractiveImageClassifier {
	
	private InteractiveImageClassifier() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final JList<Polygon> class0List = new JList<>(new DefaultListModel<Polygon>());
		final JButton addPolygonToClass0Button = new JButton("Class 0");
		final JList<Polygon> class1List = new JList<>(new DefaultListModel<Polygon>());
		final JButton addPolygonToClass1Button = new JButton("Class 1");
		final SimpleImageView imageView = new SimpleImageView();
		final Polygon polygon = new Polygon();
		
		new MouseHandler(null) {
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				polygon.reset();
				imageView.refreshBuffer();
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				polygon.addPoint(event.getX(), event.getY());
				imageView.refreshBuffer();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6747291512279656984L;
			
		}.addTo(imageView.getImageHolder());
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final Processor setPixelColorInBuffer = new Processor() {
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
						final BufferedImage buffer = component.getBufferImage();
						
						if (0 <= x && x < buffer.getWidth() && 0 <= y && y < buffer.getHeight()) {
							overlay(buffer, (int) x, (int) y, 0x80FFFF00);
						}
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = -4126445286020078292L;
					
				};
				
				forEachPixelIn(polygon, setPixelColorInBuffer);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -4910485943723290451L;
			
		});
		
		addPolygonToClass0Button.addActionListener(new AddPolygonToListAction(polygon, class0List, imageView));
		addPolygonToClass1Button.addActionListener(new AddPolygonToListAction(polygon, class1List, imageView));
		
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		show(horizontalSplit(imageView
				, verticalBox(class0List, addPolygonToClass0Button, class1List, addPolygonToClass1Button))
				, InteractiveImageClassifier.class.getSimpleName(), false);
		SwingTools.setCheckAWT(true);
	}
	
	public static final void forEachPixelIn(final Polygon polygon, final Processor processor) {
		final int n = polygon.npoints;
		
		if (n < 3) {
			return;
		}
		
		final double[] vertices = new double[n * 3];
		
		for (int i = 0; i < n; ++i) {
			vertices[i * 3 + X] = polygon.xpoints[i];
			vertices[i * 3 + Y] = polygon.ypoints[i];
		}
		
		PolygonTools.render(processor, vertices);
	}
	
	public static final void overlay(final BufferedImage image, final int x, final int y, final int argb) {
		final int rgb = image.getRGB(x, y);
		final int alpha = alpha8(argb);
		final int beta = 255 - alpha;
		final int red = (red8(rgb) * beta + red8(argb) * alpha) / 255;
		final int green = (green8(rgb) * beta + green8(argb) * alpha) / 255;
		final int blue = (blue8(rgb) * beta + blue8(argb) * alpha) / 255;
		
		image.setRGB(x, y, a8r8g8b8(0xFF, red, green, blue));
	}
	
	/**
	 * @author codistmonk (creation 2014-05-10)
	 */
	public static final class AddPolygonToListAction implements ActionListener, Serializable {
		
		private final Polygon polygon;
		
		private final JList<Polygon> list;
		
		private final SimpleImageView imageView;
		
		public AddPolygonToListAction(final Polygon polygon, final JList<Polygon> list,
				final SimpleImageView imageView) {
			this.polygon = polygon;
			this.list = list;
			this.imageView = imageView;
		}
		
		@Override
		public final void actionPerformed(final ActionEvent event) {
			if (3 <= this.polygon.npoints) {
				final DefaultListModel<Polygon> model = (DefaultListModel<Polygon>) this.list.getModel();
				
				model.addElement(new Polygon(this.polygon.xpoints, this.polygon.ypoints, this.polygon.npoints));
				
				this.polygon.reset();
				
				this.imageView.refreshBuffer();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6578501846544772646L;
		
	}
	
}
