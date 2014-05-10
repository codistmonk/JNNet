package jnnet.draft;

import static imj2.tools.IMJTools.a8r8g8b8;
import static imj2.tools.IMJTools.alpha8;
import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static pixel3d.PolygonTools.X;
import static pixel3d.PolygonTools.Y;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import nsphere.LDATest.SimpleDataset;
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
		final Context context = new Context(new SimpleImageView(), 32);
		final JButton addPolygonToClass0Button = new JButton("Class 0");
		final JButton addPolygonToClass1Button = new JButton("Class 1");
		
		new MouseHandler(null) {
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				context.getPolygon().reset();
				context.getImageView().refreshBuffer();
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				context.getPolygon().addPoint(event.getX(), event.getY());
				context.getImageView().refreshBuffer();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6747291512279656984L;
			
		}.addTo(context.getImageView().getImageHolder());
		
		context.getImageView().getPainters().add(new Painter<SimpleImageView>() {
			
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
				
				forEachPixelIn(context.getPolygon(), setPixelColorInBuffer);
				
				{
					g.setColor(Color.RED);
					
					for (final Polygon polygon : elements(context.getLists()[0])) {
						g.drawPolygon(polygon);
					}
				}
				
				{
					g.setColor(Color.GREEN);
					
					for (final Polygon polygon : elements(context.getLists()[1])) {
						g.drawPolygon(polygon);
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -4910485943723290451L;
			
		});
		
		addPolygonToClass0Button.addActionListener(new AddPolygonToListAction(context, 0));
		addPolygonToClass1Button.addActionListener(new AddPolygonToListAction(context, 1));
		
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		show(horizontalSplit(context.getImageView()
				, verticalBox(context.getLists()[0], addPolygonToClass0Button, context.getLists()[1], addPolygonToClass1Button))
				, InteractiveImageClassifier.class.getSimpleName(), false);
		SwingTools.setCheckAWT(true);
	}
	
	public static final <E> Iterable<E> elements(final JList<E> list) {
		return new Iterable<E>() {
			
			@Override
			public final Iterator<E> iterator() {
				return new Iterator<E>() {
					
					private int index = 0;
					
					@Override
					public final boolean hasNext() {
						return this.index < list.getModel().getSize();
					}
					
					@Override
					public final E next() {
						return list.getModel().getElementAt(this.index++);
					}
					
					@Override
					public final void remove() {
						@SuppressWarnings("unchecked")
						final DefaultListModel<E> model = cast(DefaultListModel.class, list.getModel());
						
						if (model == null) {
							if (list.getModel() == null) {
								throw new NullPointerException("List model undefined");
							}
							
							throw new UnsupportedOperationException(
									"expectedListModelClass: " + DefaultListModel.class.getName()
									+ " actualListModelClass: " + list.getModel().getClass().getName());
						}
						
						model.remove(this.index - 1);
					}
					
				};
			}
			
		};
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
	
	public static final double[] toItem(final BufferedImage image
			, final int x, final int y, final int windowHalfSize, final double label) {
		final int order = windowHalfSize * windowHalfSize * 4 * 3 + 1;
		final double[] result = new double[order];
		final int xEnd = x + windowHalfSize;
		final int yEnd = y + windowHalfSize;
		final int w = image.getWidth();
		final int h = image.getHeight();
		
		for (int yy = y - windowHalfSize, i = 0; yy < yEnd; ++yy) {
			for (int xx = x - windowHalfSize; xx < xEnd; ++xx, i += 3) {
				if (0 <= xx && xx < w && 0 <= yy && yy < h) {
					final int rgb = image.getRGB(xx, yy);
					
					result[i + 0] = red8(rgb);
					result[i + 1] = green8(rgb);
					result[i + 2] = blue8(rgb);
				}
			}
		}
		
		result[order - 1] = label;
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-05-10)
	 */
	public static final class AddPolygonToListAction implements ActionListener, Serializable {
		
		private final Context context;
		
		private final int label;
		
		public AddPolygonToListAction(final Context context, final int label) {
			this.context = context;
			this.label = label;
		}
		
		public final Context getContext() {
			return this.context;
		}
		
		@Override
		public final void actionPerformed(final ActionEvent event) {
			final Context context = this.getContext();
			
			if (3 <= context.getPolygon().npoints) {
				final int label = this.label;
				
				final DefaultListModel<Polygon> model =
						(DefaultListModel<Polygon>) context.getLists()[label].getModel();
				
				model.addElement(new Polygon(context.getPolygon().xpoints
						, context.getPolygon().ypoints, context.getPolygon().npoints));
				
				forEachPixelIn(context.getPolygon(), new Processor() {
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
						context.getDataset().addItem(toItem(context.getImageView().getImage()
								, (int) x, (int) y, context.getWindowHalfSize(), label));
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 5310081319263789851L;
					
				});
				
				debugPrint("datasetSize:", context.getDataset().getItemCount());
				
				context.getPolygon().reset();
				context.getImageView().refreshBuffer();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6578501846544772646L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-10)
	 */
	public static final class Context implements Serializable {
		
		private final SimpleImageView imageView;
		
		private final JList<Polygon>[] lists;
		
		private final int windowHalfSize;
		
		private final Polygon polygon;
		
		private final SimpleDataset dataset;
		
		@SuppressWarnings("unchecked")
		public Context(final SimpleImageView imageView, final int windowHalfSize) {
			this.imageView = imageView;
			this.lists = array(new JList<Polygon>(new DefaultListModel<Polygon>()), new JList<Polygon>(new DefaultListModel<Polygon>()));
			this.windowHalfSize = windowHalfSize;
			this.polygon = new Polygon();
			this.dataset = new SimpleDataset(windowHalfSize * windowHalfSize * 4 * 3 + 1);
		}
		
		public final SimpleImageView getImageView() {
			return this.imageView;
		}
		
		public final JList<Polygon>[] getLists() {
			return this.lists;
		}
		
		public final int getWindowHalfSize() {
			return this.windowHalfSize;
		}
		
		public final Polygon getPolygon() {
			return this.polygon;
		}
		
		public final SimpleDataset getDataset() {
			return this.dataset;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5900224225503382429L;
		
	}
	
}
