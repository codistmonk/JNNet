package jnnet.draft;

import static imj2.tools.IMJTools.a8r8g8b8;
import static imj2.tools.IMJTools.alpha8;
import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static jnnet.draft.InteractiveImageClassifier.ImageDataset.item;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.I18N.item;
import static net.sourceforge.aprog.swing.SwingTools.I18N.menu;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debug;
import static pixel3d.PolygonTools.X;
import static pixel3d.PolygonTools.Y;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;

import jgencode.primitivelists.IntList;
import jnnet.BinaryClassifier;
import jnnet.ConsoleMonitor;
import jnnet.Dataset;
import jnnet.SimplifiedNeuralBinaryClassifier;
import jnnet.draft.CSV2Bin.DataType;
import jnnet.draft.InteractiveImageClassifier.ImageDataset.TileTransformer;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.swing.SwingTools.I18N;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
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
	
	public static final BufferedImage fill(final BufferedImage image, final Color color) {
		final Graphics2D g = image.createGraphics();
		
		g.setColor(color);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.dispose();
		
		return image;
	}
	
	public static final ImageIcon newIcon16(final Color color) {
		return new ImageIcon(fill(new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR), color));
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final Context context = new Context(new SimpleImageView(), 32);
		final JPanel mainPanel = new JPanel(new BorderLayout());
		final JButton trainAndClassifyButton = new JButton("Classify");
		final JToolBar toolBar = new JToolBar();
		final JToggleButton clearExamplesButton = new JToggleButton(newIcon16(Color.GRAY));
		final JToggleButton negativeExamplesButton = new JToggleButton(newIcon16(Color.RED));
		final JToggleButton positiveExamplesButton = new JToggleButton(newIcon16(Color.GREEN));
		final ButtonGroup buttonGroup = new ButtonGroup();
		final Point mouseLocation = new Point(-1, -1);
		final int[] brushSize = { 8 };
		
		buttonGroup.add(clearExamplesButton);
		buttonGroup.add(negativeExamplesButton);
		buttonGroup.add(positiveExamplesButton);
		buttonGroup.setSelected(clearExamplesButton.getModel(), true);
		
		new MouseHandler(null) {
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				mouseLocation.x = -1;
				context.getImageView().refreshBuffer();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				mouseLocation.setLocation(event.getPoint());
				context.getImageView().refreshBuffer();
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.updateDataset(mouseLocation);
				
				context.getImageView().refreshBuffer();
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				final Point location = (Point) mouseLocation.clone();
				
				mouseLocation.setLocation(event.getPoint());
				
				PolygonTools.renderSegment(new Processor() {
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
						location.setLocation(x, y);
						updateDataset(location);
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 2789309737021733183L;
					
				}, location.x, location.y, 0.0, mouseLocation.x, mouseLocation.y, 0.0);
				
				context.getImageView().refreshBuffer();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (event.getWheelRotation() < 0) {
					brushSize[0] = max(1, brushSize[0] - 1);
				} else {
					brushSize[0] = min(64, brushSize[0] + 1);
				}
				
				context.getImageView().refreshBuffer();
			}
			
			final void updateDataset(final Point location) {
				final int label;
				
				if (buttonGroup.getSelection() == negativeExamplesButton.getModel()) {
					label = 0;
				} else if (buttonGroup.getSelection() == positiveExamplesButton.getModel()) {
					label = 1;
				} else {
					label = -1;
				}
				
				PolygonTools.renderDisc(new Processor() {
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 5573155945922132926L;
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
						if (0 <= label) {
							context.getDataset().addPixelItems((int) x, (int) y, label);
						} else {
							context.getDataset().removePixelItems((int) x, (int) y);
						}
					}
					
				}, location.x, location.y, 0.0, brushSize[0]);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6747291512279656984L;
			
		}.addTo(context.getImageView().getImageHolder());
		
		context.getImageView().getPainters().add(new Painter<SimpleImageView>() {
			
			private final BitSet classification = new BitSet();
			
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
					
					this.classification.clear();
					final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
					
					for (int y = 0, i = 0; y < h; ++y) {
						for (int x = 0; x < w; ++x, ++i) {
							monitor.ping(i + "/" + (w * h) + "\r");
							
							if (classifier.accept(item(image, TileTransformer.Predefined.ID, x, y
									, context.getWindowHalfSize(), Double.NaN))) {
								this.classification.set(i);
							}
						}
					}
					
					monitor.pause();
					
					context.debugPrintEnd("Classifying");
				}
				
				for (int y = 0, i = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++i) {
						overlay(buffer, x, y, this.classification.get(i) ? 0x6000FF00 : 0x60FF0000);
					}
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
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						if (context.getDataset().contains(x, y)) {
							final int label = context.getDataset().getPixelLabel(x, y);
							
							overlay(buffer, x, y, label == 0 ? 0x80FF0000 : 0x8000FF00);
						}
					}
				}
				
				if (0 <= mouseLocation.x) {
					g.setColor(Color.WHITE);
					g.drawOval(mouseLocation.x - brushSize[0] / 2, mouseLocation.y - brushSize[0] / 2
							, brushSize[0], brushSize[0]);
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -4910485943723290451L;
			
		});
		
		trainAndClassifyButton.addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				context.updateDatasetAndClassifier();
			}
			
		});
		
		SwingTools.useSystemLookAndFeel();
		SwingTools.setCheckAWT(false);
		
		final JPopupMenu exportMenu = new JPopupMenu();
		
		exportMenu.add(item("Bin...", context, "exportDatasetAsBin"));
		exportMenu.add(item("ARFF...", context, "exportDatasetAsARFF"));
		
		toolBar.add(clearExamplesButton);
		toolBar.add(negativeExamplesButton);
		toolBar.add(positiveExamplesButton);
		toolBar.addSeparator();
		toolBar.add(new AbstractAction("Export") {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				exportMenu.show((Component) event.getSource(), 0, 0);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 84824159907894088L;
			
		});
		toolBar.addSeparator();
		toolBar.add(new JPanel());
		toolBar.add(new JLabel("acceptableErrorRate:"));
		toolBar.add(context.getClassifierParameterSpinner());
		toolBar.add(trainAndClassifyButton);
		
		mainPanel.add(toolBar, BorderLayout.NORTH);
		mainPanel.add(context.getImageView(), BorderLayout.CENTER);
		
		show(mainPanel, InteractiveImageClassifier.class.getSimpleName(), false);
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
	 * @author codistmonk (creation 2014-05-11)
	 */
	public static final class ImageDataset implements Dataset {
		
		private final BufferedImage image;
		
		private final int windowHalfSize;
		
		private final int itemSize;
		
		private final BitSet pixels;
		
		private final BitSet labels;
		
		private final AtomicLong constructionTimestamp;
		
		private final AtomicLong usageTimestamp;
		
		private final IntList pixelAndLabels;
		
		private final DatasetStatistics statistics;
		
		private final List<TileTransformer> tileTransformers;
		
		public ImageDataset(final BufferedImage image, final int windowHalfSize) {
			this.image = image;
			this.windowHalfSize = windowHalfSize;
			final int tileSize = windowHalfSize * 2;
			this.itemSize = tileSize * tileSize * 3 + 1;
			this.pixels = new BitSet();
			this.labels = new BitSet();
			this.constructionTimestamp = new AtomicLong();
			this.usageTimestamp = new AtomicLong();
			this.pixelAndLabels = new IntList();
			this.statistics = new DatasetStatistics(this.itemSize - 1);
			this.tileTransformers = new ArrayList<>();
			
			this.tileTransformers.add(TileTransformer.Predefined.ID);
			this.tileTransformers.add(new TileRotation90(tileSize));
			this.tileTransformers.add(new TileRotation180(tileSize));
			this.tileTransformers.add(new TileRotation270(tileSize));
		}
		
		public final boolean contains(final int x, final int y) {
			return this.contains(this.getPixel(x, y));
		}
		
		public final boolean contains(final int pixel) {
			return this.pixels.get(pixel);
		}
		
		public final int getPixelLabel(final int x, final int y) {
			return this.getPixelLabel(this.getPixel(x, y));
		}
		
		public final int getPixelLabel(final int pixel) {
			return this.labels.get(pixel) ? 1 : 0;
		}
		
		public final int getPixel(final int x, final int y) {
			return y * this.getImage().getWidth() + x;
		}
		
		public final int[] getPixelAndLabels() {
			final long timestamp = this.constructionTimestamp.get();
			
			if (this.usageTimestamp.getAndSet(timestamp) != timestamp) {
				this.pixelAndLabels.resize(this.pixels.cardinality());
				this.pixelAndLabels.clear();
				this.getStatistics().reset();
				
				final int w = this.getImage().getWidth();
				final int n = w * this.getImage().getHeight();
				
				for (int pixel = 0; pixel < n; ++pixel) {
					if (this.contains(pixel)) {
						final int x = pixel % w;
						final int y = pixel / w;
						final int label = this.getPixelLabel(pixel);
						
						this.pixelAndLabels.addAll(pixel, label);
						
						for (final TileTransformer tileTransformer : this.tileTransformers) {
							this.getStatistics().addItem(item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize(), label));
						}
					}
				}
				
				this.getStatistics().printTo(System.out);
			}
			
			return this.pixelAndLabels.toArray();
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
		
		public final ImageDataset reset() {
			this.pixelAndLabels.clear();
			this.getStatistics().reset();
			
			return this;
		}
		
		public final ImageDataset addPixelItems(final int x, final int y, final int label) {
			if (!this.canContain(x, y)) {
				return this;
			}
			
			return this.addPixelItems(this.getPixel(x, y), label);
		}
		
		public final ImageDataset addPixelItems(final int pixel, final int label) {
			this.constructionTimestamp.incrementAndGet();
			
			this.pixels.set(pixel);
			this.labels.set(pixel, label == 1);
			
			return this;
		}
		
		public final ImageDataset removePixelItems(final int x, final int y) {
			if (!this.canContain(x, y)) {
				return this;
			}
			
			return this.removePixelItems(this.getPixel(x, y));
		}
		
		public final boolean canContain(final int x, final int y) {
			return 0 <= x && x < this.getImage().getWidth() && 0 <= y && y < this.getImage().getHeight();
		}
		
		public final ImageDataset removePixelItems(final int pixel) {
			this.constructionTimestamp.incrementAndGet();
			
			this.pixels.clear(pixel);
			
			return this;
		}
		
		@Override
		public final int getItemCount() {
			return this.getPixelAndLabels().length / 2 * this.tileTransformers.size();
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
			final int center = this.getPixelAndLabels()[untransformedItemId * 2 + 0];
			final int imageWidth = this.getImage().getWidth();
			final int imageHeight = this.getImage().getHeight();
			final int tileSize = this.getWindowHalfSize() * 2;
			final int xInTile = (valueId / 3) % tileSize;
			final int yInTile = (valueId / 3) / tileSize;
			final int x = (center % imageWidth) - this.getWindowHalfSize()
					+ tileTransformer.transformXInTile(xInTile, yInTile);
			final int y = (center / imageWidth) - this.getWindowHalfSize()
					+ tileTransformer.transformYInTile(xInTile, yInTile);
			
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
			final int pixel = this.getPixelAndLabels()[untransformedItemId * 2 + 0];
			final int label = this.getPixelAndLabels()[untransformedItemId * 2 + 1];
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
			final int pixel = this.getPixelAndLabels()[untransformedItemId * 2 + 0];
			final int imageWidth = this.getImage().getWidth();
			final int x = pixel % imageWidth;
			final int y = pixel / imageWidth;
			
			return item(this.getImage(), tileTransformer, x, y, this.getWindowHalfSize()
					, new double[this.getItemSize() - 1]);
		}
		
		@Override
		public final double getItemLabel(final int itemId) {
			return this.getPixelAndLabels()[itemId / this.tileTransformers.size() * 2 + 1];
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
			final int xStart = x - windowHalfSize;
			final int yStart = y - windowHalfSize;
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int tileEnd = 2 * windowHalfSize;
			
			Arrays.fill(result, 0.0);
			
			for (int yInTile = 0, i = 0; yInTile < tileEnd; ++yInTile) {
				for (int xInTile = 0; xInTile < tileEnd; ++xInTile, i += 3) {
					final int xx = xStart + tileTransformer.transformXInTile(xInTile, yInTile);
					final int yy = yStart + tileTransformer.transformYInTile(xInTile, yInTile);
					
					if (0 <= xx && xx < w && 0 <= yy && yy < h) {
						final int rgb = tileTransformer.transformRGB(image.getRGB(xx, yy));
						
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
		public static abstract class TileRotation implements TileTransformer {
			
			private final int tileSize;
			
			protected TileRotation(final int tileSize) {
				this.tileSize = tileSize;
			}
			
			public final int getTileSize() {
				return this.tileSize;
			}
			
			@Override
			public final int transformRGB(final int rgb) {
				return rgb;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4151623294048501227L;
			
		}
		
		/**
		 * @author codistmonk (creation 2014-05-11)
		 */
		public static final class TileRotation90 extends TileRotation {
			
			public TileRotation90(final int tileSize) {
				super(tileSize);
			}
			
			@Override
			public final int transformXInTile(final int xInTile, final int yInTile) {
				return this.getTileSize() - yInTile;
			}
			
			@Override
			public final int transformYInTile(final int xInTile, final int yInTile) {
				return xInTile;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -1450451672704459382L;
			
		}
		
		/**
		 * @author codistmonk (creation 2014-05-11)
		 */
		public static final class TileRotation180 extends TileRotation {
			
			public TileRotation180(final int tileSize) {
				super(tileSize);
			}
			
			@Override
			public final int transformXInTile(final int xInTile, final int yInTile) {
				return this.getTileSize() - xInTile;
			}
			
			@Override
			public final int transformYInTile(final int xInTile, final int yInTile) {
				return this.getTileSize() - yInTile;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6070034523633787629L;
			
		}
		
		/**
		 * @author codistmonk (creation 2014-05-11)
		 */
		public static final class TileRotation270 extends TileRotation {
			
			public TileRotation270(final int tileSize) {
				super(tileSize);
			}
			
			@Override
			public final int transformXInTile(final int xInTile, final int yInTile) {
				return yInTile;
			}
			
			@Override
			public final int transformYInTile(final int xInTile, final int yInTile) {
				return this.getTileSize() - xInTile;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 5962504140559334542L;
			
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
		
		private final JSpinner classifierParameterSpinner;
		
		private final int windowHalfSize;
		
		private ImageDataset dataset;
		
		private BinaryClassifier classifier;
		
		public Context(final SimpleImageView imageView, final int windowHalfSize) {
			this.timer = new TicToc();
			this.imageView = imageView;
			this.classifierUpdated = new AtomicBoolean();
			this.classifierParameterSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 49, 1));
			this.windowHalfSize = windowHalfSize;
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
		
		public final JSpinner getClassifierParameterSpinner() {
			return this.classifierParameterSpinner;
		}
		
		public final int getWindowHalfSize() {
			return this.windowHalfSize;
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
			
			this.getDataset().getPixelAndLabels();
			this.getDataset().getStatistics().printTo(System.out);
			
			this.debugPrintEnd("Creating dataset");
			
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
		
		public final void exportDatasetAsARFF() {
			try {
				Dataset.IO.writeARFF(this.getDataset(), new PrintStream(new BufferedOutputStream(new FileOutputStream("dataset.arff"))));
			} catch (final FileNotFoundException exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		public final void exportDatasetAsBin() {
			try {
				Dataset.IO.writeBin(this.getDataset(), DataType.BYTE, new DataOutputStream(new BufferedOutputStream(new FileOutputStream("dataset.bin"))));
			} catch (final FileNotFoundException exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5900224225503382429L;
		
	}
	
}
