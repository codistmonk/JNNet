package jnnet.draft;

import static imj2.tools.IMJTools.a8r8g8b8;
import static imj2.tools.IMJTools.alpha8;
import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static java.util.Arrays.fill;
import static jnnet.draft.InteractiveImageClassifier.ImageDataset.item;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debug;
import static pixel3d.PolygonTools.X;
import static pixel3d.PolygonTools.Y;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jgencode.primitivelists.IntList;
import jnnet.BinaryClassifier;
import jnnet.ConsoleMonitor;
import jnnet.Dataset;
import jnnet.SimplifiedNeuralBinaryClassifier;
import jnnet.draft.InteractiveImageClassifier.ImageDataset.TileTransformer;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
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
		final JButton trainAndClassifyButton = new JButton("Train and classify");
		
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
				final BufferedImage image = component.getImage();
				final BufferedImage buffer = component.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				final BinaryClassifier classifier = context.getClassifier();
				
				if (classifier != null && context.getClassifierUpdated().getAndSet(false)) {
					context.debugPrintBegin("Classifying");
					
					final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
					
					for (int y = 0, i = 0; y < h; ++y) {
						for (int x = 0; x < w; ++x, ++i) {
							monitor.ping(i + "/" + (w * h) + "\r");
							
							if (classifier.accept(item(image, TileTransformer.Predefined.ID, x, y
									, context.getWindowHalfSize(), Double.NaN))) {
								overlay(buffer, x, y, 0x6000FF00);
							} else {
								overlay(buffer, x, y, 0x60FF0000);
							}
						}
					}
					
					monitor.pause();
					
					context.debugPrintEnd("Classifying");
				}
				
				final Processor setPixelColorInBuffer = new Processor() {
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
						if (0 <= x && x < w && 0 <= y && y < h) {
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
		trainAndClassifyButton.addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				context.updateDatasetAndClassifier();
			}
			
		});
		
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		show(horizontalSplit(context.getImageView()
				, verticalBox(scrollable(context.getLists()[0])
						, addPolygonToClass0Button
						, Box.createVerticalGlue()
						, scrollable(context.getLists()[1])
						, addPolygonToClass1Button
						, Box.createVerticalGlue()
						, horizontalBox(new JLabel("acceptableErrorRate:")
							, context.getClassifierParameterSpinner(), trainAndClassifyButton)
				))
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
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static final class ImageDataset implements Dataset {
		
		private final BufferedImage image;
		
		private final int windowHalfSize;
		
		private final int itemSize;
		
		private final IntList pixelAndLabels;
		
		private final DatasetStatistics statistics;
		
		private final List<TileTransformer> tileTransformers;
		
		public ImageDataset(final BufferedImage image, final int windowHalfSize) {
			this.image = image;
			this.windowHalfSize = windowHalfSize;
			this.itemSize = windowHalfSize * windowHalfSize * 4 * 3 + 1;
			this.pixelAndLabels = new IntList();
			this.statistics = new DatasetStatistics(this.itemSize - 1);
			this.tileTransformers = new ArrayList<>();
			
			this.tileTransformers.add(TileTransformer.Predefined.ID);
		}
		
		public final BufferedImage getImage() {
			return this.image;
		}
		
		public final int getWindowHalfSize() {
			return this.windowHalfSize;
		}
		
		public final DatasetStatistics getStatistics() {
			return this.statistics;
		}
		
		public final ImageDataset addItem(final int x, final int y, final int label) {
			return this.addItem(y * this.getImage().getWidth() + x, label);
		}
		
		public final ImageDataset addItem(final int pixel, final int label) {
			final int itemId = this.getItemCount();
			this.pixelAndLabels.addAll(pixel, label);
			
			this.getStatistics().addItem(this.getItem(itemId));
			
			return this;
		}
		
		@Override
		public final int getItemCount() {
			return this.pixelAndLabels.size() / 2 * this.tileTransformers.size();
		}
		
		@Override
		public final int getItemSize() {
			return this.itemSize;
		}
		
		@Override
		public final double getItemValue(final int itemId, final int valueId) {
			final TileTransformer tileTransformer =
					this.tileTransformers.get(itemId % this.tileTransformers.size());
			final int untransformedItemId = itemId / this.tileTransformers.size();
			final int center = this.pixelAndLabels.get(untransformedItemId * 2 + 0);
			final int imageWidth = this.getImage().getWidth();
			final int imageHeight = this.getImage().getHeight();
			final int tileWidth = this.getWindowHalfSize() * 2;
			final int xInTile = (valueId / 3) % tileWidth;
			final int yInTile = (valueId / 3) / tileWidth;
			final int x = (center % imageWidth) + tileTransformer.transformXInTile(xInTile, yInTile);
			final int y = (center / imageWidth) + tileTransformer.transformYInTile(xInTile, yInTile);
			
			if (x < 0 || imageWidth <= x || y < 0 || imageHeight <= y) {
				return 0.0;
			}
			
			final int rgb = tileTransformer.transformRGB(this.getImage().getRGB(x, y));
			
			switch (valueId % 3) {
			case 0:
				return red8(rgb);
			case 1:
				return green8(rgb);
			case 2:
				return blue8(rgb);
			}
			
			throw new IllegalStateException();
		}
		
		@Override
		public final double[] getItem(final int itemId) {
			final TileTransformer tileTransformer =
					this.tileTransformers.get(itemId % this.tileTransformers.size());
			final int untransformedItemId = itemId / this.tileTransformers.size();
			final int pixel = this.pixelAndLabels.get(untransformedItemId * 2 + 0);
			final int label = this.pixelAndLabels.get(untransformedItemId * 2 + 1);
			final int imageWidth = this.getImage().getWidth();
			final int x = pixel % imageWidth;
			final int y = pixel / imageWidth;
			
			return item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize(), label);
		}
		
		@Override
		public final double[] getItemWeights(final int itemId) {
			final TileTransformer tileTransformer =
					this.tileTransformers.get(itemId % this.tileTransformers.size());
			final int untransformedItemId = itemId / this.tileTransformers.size();
			final int pixel = this.pixelAndLabels.get(untransformedItemId * 2 + 0);
			final int imageWidth = this.getImage().getWidth();
			final int x = pixel % imageWidth;
			final int y = pixel / imageWidth;
			
			return item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize()
					, new double[this.getItemSize() - 1]);
		}
		
		@Override
		public final double getItemLabel(final int itemId) {
			return this.pixelAndLabels.get(itemId * 2 + 1);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8970225174892151354L;
		
		public static final double[] item(final BufferedImage image, final TileTransformer tileTransformer
				, final int x, final int y, final int windowHalfSize, final double label) {
			final int order = windowHalfSize * windowHalfSize * 4 * 3 + 1;
			final double[] result = item(image, tileTransformer, x, y, windowHalfSize, new double[order]);
			
			result[order - 1] = label;
			
			return result;
		}
		
		public static final double[] item(final BufferedImage image, final TileTransformer tileTransformer
				, final int x, final int y, final int windowHalfSize, final double[] result) {
			final int xEnd = x + windowHalfSize;
			final int yEnd = y + windowHalfSize;
			final int w = image.getWidth();
			final int h = image.getHeight();
			
			fill(result, 0.0);
			
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
			
			return result;
		}
		
		/**
		 * @author codistmonk (creation 2014-05-11)
		 */
		public static abstract interface TileTransformer extends Serializable {
			
			public abstract int transformXInTile(int xInTile, int yInTile);
			
			public abstract int transformYInTile(int xInTile, int yInTile);
			
			public abstract int transformRGB(int rgb);
			
			/**
			 * @author codistmonk (creation 2014-05-11)
			 */
			public static enum Predefined implements TileTransformer {
				
				ID {
					
					@Override
					public final int transformXInTile(final int xInTile, final int yInTile) {
						return xInTile;
					}
					
					@Override
					public final int transformYInTile(final int xInTile, final int yInTile) {
						return yInTile;
					}
					
					@Override
					public final int transformRGB(final int rgb) {
						return rgb;
					}
					
				};
				
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-10)
	 */
	public static final class Context implements Serializable {
		
		private final TicToc timer;
		
		private final SimpleImageView imageView;
		
		private final AtomicBoolean classifierUpdated;
		
		private final JList<Polygon>[] lists;
		
		private final JSpinner classifierParameterSpinner;
		
		private final int windowHalfSize;
		
		private final Polygon polygon;
		
		private ImageDataset dataset;
		
		private BinaryClassifier classifier;
		
		@SuppressWarnings("unchecked")
		public Context(final SimpleImageView imageView, final int windowHalfSize) {
			this.timer = new TicToc();
			this.imageView = imageView;
			this.classifierUpdated = new AtomicBoolean();
			this.lists = array((JList<Polygon>) newJList(this), (JList<Polygon>) newJList(this));
			this.classifierParameterSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 49, 1));
			this.windowHalfSize = windowHalfSize;
			this.polygon = new Polygon();
		}
		
		public final TicToc getTimer() {
			return this.timer;
		}
		
		public final AtomicBoolean getClassifierUpdated() {
			return this.classifierUpdated;
		}
		
		public final BinaryClassifier getClassifier() {
			return this.classifier;
		}
		
		public final SimpleImageView getImageView() {
			return this.imageView;
		}
		
		public final JList<Polygon>[] getLists() {
			return this.lists;
		}
		
		public final JSpinner getClassifierParameterSpinner() {
			return this.classifierParameterSpinner;
		}
		
		public final int getWindowHalfSize() {
			return this.windowHalfSize;
		}
		
		public final Polygon getPolygon() {
			return this.polygon;
		}
		
		public final ImageDataset getDataset() {
			final BufferedImage image = this.getImageView().getImage();
			
			if (this.dataset == null || this.dataset.getImage() != image) {
				this.dataset = new ImageDataset(image, this.getWindowHalfSize());
			}
			
			return this.dataset;
		}
		
		public final void updateDatasetAndClassifier() {
			this.debugPrintBegin("Creating dataset");
			
			for (final int label : new int[] { 0, 1 }) {
				for (final Polygon polygon : elements(Context.this.getLists()[label])) {
					forEachPixelIn(polygon, new Processor() {
						
						@Override
						public final void pixel(final double x, final double y, final double z) {
							Context.this.getDataset().addItem((int) x, (int) y, label);
						}
						
						/**
						 * {@value}.
						 */
						private static final long serialVersionUID = 5310081319263789851L;
						
					});
				}
			}
			
			this.debugPrintEnd("Creating dataset");
			
			this.getDataset().getStatistics().printTo(System.out);
			
			this.updateClassifier();
		}
		
		public final void updateClassifier() {
			this.debugPrintBegin("Creating classifier");
			
			final double acceptableErrorRate = ((Number) this.getClassifierParameterSpinner().getValue())
					.doubleValue() / 100.0;
			
			this.classifier = new SimplifiedNeuralBinaryClassifier(this.getDataset()
					, 0.5, acceptableErrorRate, Integer.MAX_VALUE, true, true);
			
			this.debugPrintEnd("Creating classifier");
			
			this.getClassifierUpdated().set(true);
			
			this.getImageView().refreshBuffer();
		}
		
		public final void debugPrintBegin(final String operation) {
			System.out.println(debug(DEBUG_STACK_OFFSET + 1, operation, "started...", new Date(this.getTimer().tic())));
		}
		
		public final void debugPrintEnd(final String operation) {
			System.out.println(debug(DEBUG_STACK_OFFSET + 1, operation, "done in", this.getTimer().toc(), "ms"));
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5900224225503382429L;
		
		public static final JList<?> newJList(final Context context) {
			final JList<?> result = new JList<>(new DefaultListModel<>());
			
			result.addKeyListener(new KeyAdapter() {
				
				@Override
				public final void keyPressed(final KeyEvent event) {
					switch (event.getKeyCode()) {
					case KeyEvent.VK_DELETE:
					case KeyEvent.VK_BACK_SPACE:
						final DefaultListModel<?> model = (DefaultListModel<?>) result.getModel();
						
						for (final Object toRemove : result.getSelectedValuesList()) {
							model.removeElement(toRemove);
						}
						
						context.getImageView().refreshBuffer();
						
						break;
					}
				}
				
			});
			
			return result;
		}
		
	}
	
}
