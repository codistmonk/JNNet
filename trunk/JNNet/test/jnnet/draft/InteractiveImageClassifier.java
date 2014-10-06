package jnnet.draft;

import static imj2.pixel3d.PolygonTools.X;
import static imj2.pixel3d.PolygonTools.Y;
import static imj2.tools.IMJTools.a8r8g8b8;
import static imj2.tools.IMJTools.alpha8;
import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static jnnet.draft.ImageDataset.item;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.swing.SwingTools.I18N.item;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debug;
import imj2.pixel3d.MouseHandler;
import imj2.pixel3d.PolygonTools;
import imj2.pixel3d.PolygonTools.Processor;
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
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;

import jnnet.BinaryClassifier;
import jnnet.Dataset;
import jnnet.SimplifiedNeuralBinaryClassifier;
import jnnet.draft.CSV2Bin.DataType;
import jnnet.draft.ImageDataset.TileTransformer;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.ConsoleMonitor;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TaskManager;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-05-10)
 */
public final class InteractiveImageClassifier {
	
	private InteractiveImageClassifier() {
		throw new IllegalInstantiationException();
	}
	
	public static final int GRID = 2;
	
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
						if ((((int) x) % GRID) != 0 || (((int) y) % GRID) != 0) {
							return;
						}
						
						
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
			
			private final BitSet[] classification = { null };
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final BufferedImage image = component.getImage();
				final BufferedImage buffer = component.getBufferImage();
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				
				if (0 == context.getDataset().getItemCount()) {
					this.classification[0] = null;
				}
				
				final BinaryClassifier classifier = context.getClassifier();
				
				if (classifier != null && context.getClassifierUpdated().getAndSet(false)) {
					context.debugPrintBegin("Classifying");
					
					if (this.classification[0] == null) {
						this.classification[0] = new BitSet();
					}
					
					this.classification[0].clear();
					
					final ConsoleMonitor monitor = new ConsoleMonitor(10000L);
					final TaskManager taskManager = new TaskManager();
					final AtomicInteger count = new AtomicInteger();
					
					for (int y0 = 0; y0 < h; y0 += GRID) {
						final int y = y0;
						final int pixel0 = y * w;
						
						taskManager.submit(new Runnable() {
							
							@Override
							public final void run() {
								for (int x = 0; x < w; x += GRID) {
									monitor.ping(count.incrementAndGet() + "/" + (w * h) + "\r");
									
									if (classifier.accept(item(image, TileTransformer.Predefined.ID, x, y
											, context.getWindowHalfSize(), Double.NaN))) {
										synchronized (classification[0]) {
											classification[0].set(pixel0 + x);
										}
									}
								}
							}
							
						});
					}
					
					taskManager.join();
					
					monitor.pause();
					
					context.debugPrintEnd("Classifying");
				}
				
				if (this.classification[0] != null) {
					for (int y = 0; y < h; y += GRID) {
						for (int x = 0, pixel = y * w; x < w; x += GRID, pixel += GRID) {
							overlay(buffer, x, y, this.classification[0].get(pixel) ? 0x6000FF00 : 0x60FF0000);
						}
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
				
				for (int y = 0; y < h; y += GRID) {
					for (int x = 0; x < w; x += GRID) {
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
