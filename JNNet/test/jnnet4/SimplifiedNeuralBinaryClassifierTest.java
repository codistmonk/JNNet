package jnnet4;

import static java.lang.Double.parseDouble;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.disjoint;
import static java.util.Collections.swap;
import static jnnet4.FeedforwardNeuralNetworkTest.intersection;
import static jnnet4.JNNetTools.ATOMIC_INTEGER_FACTORY;
import static jnnet4.JNNetTools.RANDOM;
import static jnnet4.JNNetTools.rgb;
import static jnnet4.LinearConstraintSystemTest.LinearConstraintSystem.unscale;
import static jnnet4.ProjectiveClassifier.preview;
import static jnnet4.VectorStatistics.add;
import static jnnet4.VectorStatistics.dot;
import static jnnet4.VectorStatistics.scaled;
import static jnnet4.VectorStatistics.subtract;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_MAP_FACTORY;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_SET_FACTORY;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.join;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import jnnet.DoubleList;
import jnnet4.BinaryClassifier.EvaluationMonitor;
import jnnet4.LinearConstraintSystemTest.LinearConstraintSystem;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class SimplifiedNeuralBinaryClassifierTest {
	
	/**
	 * @author codistmonk (creation 2014-03-25)
	 */
	public static final class MNISTErrorMonitor implements EvaluationMonitor {
		
		private final Collection<BufferedImage> falseNegatives;
		
		private final Collection<BufferedImage> falsePositives;
		
		private final Dataset trainingData;
		
		private final int maximumImagesPerCategory;
		
		public MNISTErrorMonitor(final Dataset dataset, final int maximumImagesPerCategory) {
			this.falseNegatives = new ArrayList<BufferedImage>(maximumImagesPerCategory);
			this.falsePositives = new ArrayList<BufferedImage>(maximumImagesPerCategory);
			this.trainingData = dataset;
			this.maximumImagesPerCategory = maximumImagesPerCategory;
		}
		
		public final Collection<BufferedImage> getFalseNegatives() {
			return this.falseNegatives;
		}
		
		public final Collection<BufferedImage> getFalsePositives() {
			return this.falsePositives;
		}
		
		@Override
		public final void truePositive(final int sampleId) {
			ignore(sampleId);
		}
		
		@Override
		public final void trueNegative(final int sampleId) {
			ignore(sampleId);
		}
		
		@Override
		public final void falsePositive(final int sampleId) {
			this.getImage(sampleId, this.falsePositives);
		}
		
		@Override
		public final void falseNegative(final int sampleId) {
			this.getImage(sampleId, this.falseNegatives);
		}
		
		private final void getImage(final int sampleId, final Collection<BufferedImage> out) {
			if (out.size() < this.maximumImagesPerCategory) {
				final int step = this.trainingData.getStep();
				final int w = (int) sqrt(step - 1);
				final int h = w;
				final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				
				for (int y = 0, p = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++p) {
						image.setRGB(x, y, rgb(this.trainingData.getData()[sampleId * step + p] / 255.0));
					}
				}
				
				out.add(image);
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3549752842564996266L;
		
	}

//	@Test
	public final void test1() {
		final boolean showClassifier = true;
		final boolean previewTrainingData = false;
		final boolean previewValidationData = false;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
//		final Dataset trainingData = new Dataset("jnnet/2spirals.txt");
//		final Dataset trainingData = new Dataset("jnnet/iris_versicolor.txt");
		final Dataset trainingData = new Dataset("../Libraries/datasets/gisette/gisette_train.data");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 0, 500000);
//		final Dataset trainingData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 0, 500000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (previewTrainingData) {
			SwingTools.show(preview(trainingData, 8), "Training data", false);
		}
		
		debugPrint("Loading validation dataset started", new Date(timer.tic()));
		final Dataset validationData = new Dataset("../Libraries/datasets/gisette/gisette_valid.data");
		debugPrint("Loading validation dataset done in", timer.toc(), "ms");
		
		if (previewValidationData) {
			SwingTools.show(preview(validationData, 8), "Validation data", false);
		}
		
//		debugPrint("Loading test dataset started", new Date(timer.tic()));
////		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 11000000-500000, 500000);
////		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 500000, 500000);
////		final Dataset testData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 5000000-500000, 500000);
//		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		for (int maximumHyperplaneCount = 2; maximumHyperplaneCount <= 200; maximumHyperplaneCount += 2) {
			debugPrint("Building classifier started", new Date(timer.tic()));
			final BinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, maximumHyperplaneCount, true, true);
			debugPrint("Building classifier done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
			final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData, null);
			debugPrint("training:", confusionMatrix);
			debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on validation set started", new Date(timer.tic()));
			debugPrint("test:", classifier.evaluate(validationData, null));
			debugPrint("Evaluating classifier on validation set done in", timer.toc(), "ms");
			
//			debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
//			debugPrint("test:", classifier.evaluate(testData));
//			debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
			
			if (showClassifier && classifier.getInputDimension() == 2) {
				show(classifier, 256, 16.0, trainingData.getData());
			}
		}
		
//		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
//	@Test
	public final void test2() throws Exception {
		final boolean showClassifier = true;
		final boolean previewTrainingData = false;
		final TicToc timer = new TicToc();
		final int digit = 9;
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final Dataset trainingData = new Dataset("../Libraries/datasets/mnist/mnist_" + digit + ".train");
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (false) {
			final double[] data = trainingData.getData();
			final int n = data.length;
			final int step = trainingData.getStep();
			
			new File("mnist/" + digit).mkdirs();
			new File("mnist/not" + digit).mkdirs();
			
			for (int i = 0, k = 500; i < n && 0 < --k; i += step) {
				final int label = (int) data[i + step - 1];
				ImageIO.write(newImage(data, i, 28, 28), "png", new File("mnist/" + (label == 0 ? "not" : "") + digit + "/mnist_" + digit + "_training_" + (i / step) + ".png"));
			}
		}
		
		if (previewTrainingData) {
			SwingTools.show(preview(trainingData, 8), "Training data", false);
		}
		
		debugPrint("Loading test dataset started", new Date(timer.tic()));
		final Dataset testData = new Dataset("../Libraries/datasets/mnist/mnist_" + digit + ".test");
		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		final MNISTErrorMonitor trainingMonitor = new MNISTErrorMonitor(trainingData, 0);
		final MNISTErrorMonitor testMonitor = new MNISTErrorMonitor(testData, 0);
		
		for (int maximumHyperplaneCount = 10; maximumHyperplaneCount <= 10; maximumHyperplaneCount += 2) {
			debugPrint("Building classifier started", new Date(timer.tic()));
			final SimplifiedNeuralBinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, maximumHyperplaneCount, true, true);
			debugPrint("Building classifier done in", timer.toc(), "ms");
			
			if (true) {
				debugPrint("Inverting classifier started", new Date(timer.tic()));
				
				final MosaicBuilder examples = new MosaicBuilder();
				final int step = classifier.getInputDimension() + 1;
				final int w = (int) sqrt(step - 1);
				final int h = w;
				final double[] hyperplanes = classifier.getHyperplanes();
				final int n = hyperplanes.length;
				
				for (final BitSet code : classifier.getClusters()) {
					debugPrint(code);
					
					final LinearConstraintSystem system = new LinearConstraintSystem(step);
					
					if (true) {
						final double[] constraint = new double[step];
						
						for (int i = 0; i < step; ++i) {
							constraint[i] = 1.0;
							system.addConstraint(constraint);
							constraint[i] = 0.0;
						}
						
						constraint[0] = 255.0;
						
						for (int i = 1; i < step; ++i) {
							constraint[i] = -1.0;
							system.addConstraint(constraint);
							constraint[i] = 0.0;
						}
					}
					
					for (int i = 0, bit = 0; i < n; i += step, ++bit) {
						final double scale = code.get(bit) ? 1.0 : -1.0;
						final double[] constraint = new double[step];
						
						for (int j = i; j < i + step; ++j) {
							constraint[j - i] = scale * hyperplanes[j];
						}
						
						system.addConstraint(constraint);
					}
					
					if (false) {
						Tools.writeObject(system, "test/jnnet4/mnist" + digit + "_system.jo");
					}
					
					final double[] example = unscale(system.solve2());
					
					debugPrint(system.accept(example));
					
					examples.getImages().add(newImage(example, 1, w, h));
					
					if (100 <= examples.getImages().size()) {
						break;
					}
				}
				
				debugPrint("Inverting classifier done in", timer.toc(), "ms");
				
				{
					final BufferedImage mosaic = examples.generateMosaic();
					
					ImageIO.write(mosaic, "png", new File("mnsit_" + digit + "_mosaic.png"));
					
					SwingTools.show(mosaic, "Cluster representatives", false);
				}
			}
			
			debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
			final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData, trainingMonitor);
			
			debugPrint("training:", confusionMatrix);
			debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
			debugPrint("test:", classifier.evaluate(testData, testMonitor));
			debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
			
			if (showClassifier && classifier.getInputDimension() == 2) {
				show(classifier, 256, 16.0, trainingData.getData());
			}
		}
		
		{
			show(trainingMonitor, "training");
			show(testMonitor, "test");
			
			gc(10000L);
		}
		
//		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
	@Test
	public final void test3() throws Exception {
		final String root = "F:/icpr2014_mitos_atypia/A03";
		final File frames40 = new File(root + "/frames/x40/");
		final File mitosis = new File(root + "/mitosis/");
		final Map<String, VirtualImage40> images = new HashMap<String, VirtualImage40>();
		
		for (final String file : frames40.list()) {
			final String tileId = file.replaceAll("\\..+$", "");
			final String basePath = new File(frames40, tileId.substring(0, tileId.length() - 2)).toString();
			
			debugPrint(tileId, basePath);
			
//			final BufferedImage image = ImageIO.read(new File(root + "/frames/x40/"+ tileId + ".png"));
//			final VirtualImage40 image = new VirtualImage40(basePath);
			final VirtualImage40 image = getOrCreate(images, basePath,
					new DefaultFactory<VirtualImage40>(VirtualImage40.class, basePath));
			
			debugPrint(image.getWidth(), image.getHeight());
			
			final PrintStream out = new PrintStream(new File(mitosis, tileId + ".data"));
			
			try {
				convert(tileId, image, new File(mitosis, tileId + "_mitosis.csv"), out);
				convert(tileId, image, new File(mitosis, tileId + "_not_mitosis.csv"), out);
			} finally {
				out.close();
			}
		}
	}
	
	public static final void convert(final String tileId, final VirtualImage40 image, final File csv, final PrintStream out) throws FileNotFoundException {
		final Scanner scanner = new Scanner(csv);
		final int windowSize = 7;
		
		try {
			while (scanner.hasNext()) {
				final String[] line = scanner.nextLine().split(",");
				final int x0 = (int) parseDouble(line[0]);
				final int y0 = (int) parseDouble(line[1]);
				final double score = parseDouble(line[2]);
				final int label = score < 0.5 ? 0 : 1;
				
				for (int y = y0 - windowSize; y < y0 + windowSize; ++y) {
					for (int x = x0 - windowSize; x < x0 + windowSize; ++x) {
						final Color color = new Color(image.getRGB(tileId, x, y));
						
						out.print(color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " ");
					}
				}
				
				out.println(label);
				
//				line[2] = score < 0.5 ? "0" : "1";
//				
//				out.println(join(" ", line));
				
			}
		} finally {
			scanner.close();
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-03-30)
	 */
	public static final class VirtualImage40 implements Serializable {
		
		private final String basePath;
		
		private final BufferedImage[][] tiles;
		
		private final int width;
		
		private final int height;
		
		public VirtualImage40(final String basePath) {
			this.basePath = basePath;
			this.tiles = new BufferedImage[4][4];
			
			for (int quad0 = 0; quad0 <= 3; ++quad0) {
				for (int quad1 = 0; quad1 <= 3; ++quad1) {
					this.tiles[quad0][quad1] = readTile(quad0, quad1);
				}
			}
			
			this.width = this.tiles[0][0].getWidth() + this.tiles[0][1].getWidth() + this.tiles[1][0].getWidth() + this.tiles[1][1].getWidth();
			this.height = this.tiles[0][0].getHeight() + this.tiles[0][2].getHeight() + this.tiles[2][0].getHeight() + this.tiles[2][2].getHeight();
		}
		
		public final int getWidth() {
			return this.width;
		}
		
		public final int getHeight() {
			return this.height;
		}
		
		public final int getRGB(final String tileId, final int x, final int y) {
			int quad0 = tileId.charAt(tileId.length() - 2) - 'A';
			int quad1 = tileId.charAt(tileId.length() - 1) - 'a';
			
			BufferedImage tile = this.tiles[quad0][quad1];
			int w = tile.getWidth();
			int h = tile.getHeight();
			
			try {
				if (x < 0) {
					if (quad1 == 1 || quad1 == 3) {
						--quad1;
					} else {
						++quad1;
						
						if (quad0 == 1 || quad0 == 3) {
							--quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				} else if (w <= x) {
					if (quad1 == 0 || quad1 == 2) {
						++quad1;
					} else {
						--quad1;
						
						if (quad0 == 0 || quad0 == 2) {
							++quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				}
				
				if (y < 0) {
					if (quad1 == 2 || quad1 == 3) {
						--quad1;
					} else {
						++quad1;
						
						if (quad0 == 2 || quad0 == 3) {
							--quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				} else if (h <= y) {
					if (quad1 == 0 || quad1 == 1) {
						++quad1;
					} else {
						--quad1;
						
						if (quad0 == 0 || quad0 == 1) {
							++quad0;
						} else {
							throw new IllegalArgumentException();
						}
					}
				}
			} catch (final IllegalArgumentException exception) {
				ignore(exception);
				
				System.err.println(debug(DEBUG_STACK_OFFSET, tileId, x, y));
				
				return 0;
			}
			
			tile = this.tiles[quad0][quad1];
			w = tile.getWidth();
			h = tile.getHeight();
			
			return tile.getRGB((w + x) % w, (h + y) % h);
		}
		
		private final BufferedImage readTile(final int quad0, final int quad1) {
			try {
				final File file = new File(this.basePath + (char) ('A' + quad0) + "" + (char) ('a' + quad1) + ".png");
				
				return ImageIO.read(file);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 5357895005657732226L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-03-28)
	 */
	public static final class MosaicBuilder implements Serializable {
		
		private final List<BufferedImage> images;
		
		private final int preferredRowCount;
		
		public MosaicBuilder() {
			this(0);
		}
		
		public MosaicBuilder(final int preferredRowCount) {
			this.images = new ArrayList<BufferedImage>();
			this.preferredRowCount = preferredRowCount;
		}
		
		public final List<BufferedImage> getImages() {
			return this.images;
		}
		
		public final int getPreferredRowCount() {
			return this.preferredRowCount;
		}
		
		public final BufferedImage generateMosaic() {
			if (this.getImages().isEmpty()) {
				return new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
			}
			final int rowCount = 0 < this.getPreferredRowCount() ? this.getPreferredRowCount() :
				(int) ceil(sqrt(this.getImages().size()));
			final int columnCount = (int) ceil((double) this.getImages().size() / rowCount);
			
			debugPrint(this.getImages().size(), rowCount, columnCount);
			
			final int imageWidth = this.getImages().get(0).getWidth();
			final int imageHeight = this.getImages().get(0).getHeight();
			final int mosaicWidth = columnCount * imageWidth;
			final int mosaicHeight = columnCount * imageHeight;
			final BufferedImage result = new BufferedImage(mosaicWidth, mosaicHeight, BufferedImage.TYPE_3BYTE_BGR);
			
			{
				final Graphics2D g = result.createGraphics();
				int i = 0;
				
				for (final BufferedImage image : this.getImages()) {
					final int tileX = (i % columnCount) * imageWidth;
					final int tileY = (i / columnCount) * imageHeight;
					
					g.drawImage(image, tileX, tileY, null);
					++i;
				}
				
				g.dispose();
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3610143890164242310L;
		
	}
	
	public static final BufferedImage newImage(final double[] example, final int offset, final int w, final int h) {
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		
		for (int y = 0, p = offset; y < h; ++y) {
			for (int x = 0; x < w; ++x, ++p) {
				image.setRGB(x, y, rgb(max(0.0, min(example[p] / example[0] / 255.0, 1.0))));
			}
		}
		return image;
	}
	
	public static final void show(final MNISTErrorMonitor trainingMonitor,
			final String datasetType) {
		for (final BufferedImage image : trainingMonitor.getFalseNegatives()) {
			SwingTools.show(image, "A " + datasetType +" false negative", false);
		}
		
		for (final BufferedImage image : trainingMonitor.getFalsePositives()) {
			SwingTools.show(image, "A " + datasetType +" false positive", false);
		}
	}
	
	public static final void show(final BinaryClassifier classifier, final int imageSize, final double scale, final double[] trainingData) {
		final TicToc timer = new TicToc();
		debugPrint("Allocating rendering buffer started", new Date(timer.tic()));
		final BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_3BYTE_BGR);
		debugPrint("Allocating rendering buffer done in", timer.toc(), "ms");
		
		debugPrint("Rendering started", new Date(timer.tic()));
		
		draw(classifier, image, scale);
		
		if (trainingData != null) {
			final Graphics2D g = image.createGraphics();
			final int inputDimension = classifier.getInputDimension();
			
			for (int i = 0; i < trainingData.length; i += inputDimension + 1) {
				final double label = trainingData[i + inputDimension];
				
				if (label < 0.5) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.GREEN);
				}
				
				g.drawOval(
						imageSize / 2 + (int) (trainingData[i + 0] * scale) - 2,
						imageSize / 2 - (int) (trainingData[i + 1] * scale) - 2,
						4, 4);
			}
			
			g.dispose();
		}
		
		debugPrint("Rendering done in", timer.toc(), "ms");
		
		SwingTools.show(image, getCallerClass().getName(), true);
	}
	
	public static final void draw(final BinaryClassifier classifier, final BufferedImage image, final double scale) {
		final int inputDimension = classifier.getInputDimension();
		
		if (inputDimension != 1 && inputDimension != 2) {
			throw new IllegalArgumentException();
		}
		
		final int w = image.getWidth();
		final int h = image.getHeight();
		final double[] input = new double[inputDimension];
		
		for (int y = 0; y < h; ++y) {
			if (1 < inputDimension) {
				input[1] = (h / 2.0 - y) / scale;
			}
			
			for (int x = 0; x < w; ++x) {
				input[0] = (x - w / 2.0) / scale;
				final double output = classifier.accept(input) ? 1.0 : 0.0;
				
				if (inputDimension == 1) {
					final int yy = (int) (h / 2 - scale * output);
					image.setRGB(x, yy, Color.WHITE.getRGB());
				} else if (inputDimension == 2) {
					image.setRGB(x, y, 0xFF000000 | (JNNetTools.uint8(output) * 0x00010101));
				}
			}
			
			if (inputDimension == 1) {
				break;
			}
		}
	}
	
}

/**
 * @author codistmonk (creation 2014-03-10)
 */
final class SimplifiedNeuralBinaryClassifier implements BinaryClassifier {
	
	private final int inputDimension;
	
	private final double[] hyperplanes;
	
	private final Collection<BitSet> clusters;
	
	private final boolean invertOutput;
	
	public SimplifiedNeuralBinaryClassifier(final Dataset trainingDataset) {
		this(trainingDataset, Integer.MAX_VALUE, true, true);
	}
	
	public SimplifiedNeuralBinaryClassifier(final Dataset trainingDataset, final int maximumHyperplaneCount,
			final boolean allowHyperplanePruning, final boolean allowOutputInversion) {
		debugPrint("Partitioning...");
		
		final DoubleList hyperplanes = new DoubleList();
		final int step = trainingDataset.getStep();
		
		generateHyperplanes(trainingDataset, new HyperplaneHandler() {
			
			@Override
			public final boolean hyperplane(final double bias, final double[] weights) {
				hyperplanes.add(bias);
				hyperplanes.addAll(weights);
				
				return hyperplanes.size() / step < maximumHyperplaneCount;
			}
			
			/**
			 * {@values}.
			 */
			private static final long serialVersionUID = 664820514870575702L;
			
		});
		
		final double[] data = trainingDataset.getData();
		this.inputDimension = step - 1;
		final Codeset codes = cluster(hyperplanes.toArray(), data, step);
		
		{
			final Collection<BitSet> ambiguousCodes = intersection(new HashSet<BitSet>(codes.getCodes()[0].keySet()), codes.getCodes()[1].keySet());
			
			if (!ambiguousCodes.isEmpty()) {
				System.err.println(debug(Tools.DEBUG_STACK_OFFSET, "ambiguities:", ambiguousCodes.size()));
				
				for (final BitSet ambiguousCode : ambiguousCodes) {
					if (codes.getCodes()[0].get(ambiguousCode).get() <= codes.getCodes()[1].get(ambiguousCode).get()) {
						codes.getCodes()[0].remove(ambiguousCode);
					} else {
						codes.getCodes()[1].remove(ambiguousCode);
					}
				}
				
				System.err.println(debug(Tools.DEBUG_STACK_OFFSET, codes));
				
				Tools.gc(1L);
			}
		}
		
		if (allowHyperplanePruning) {
			removeHyperplanes(codes.prune(), hyperplanes, step);
		}
		
		this.hyperplanes = hyperplanes.toArray();
		this.invertOutput = allowOutputInversion && codes.getCodes()[0].size() < codes.getCodes()[1].size();
		this.clusters = this.invertOutput ? codes.getCodes()[0].keySet() : codes.getCodes()[1].keySet();
		
		if (false) {
			debugPrint("Experimental section...");
			
			Codeset higherLevelCodes = codes;
			
			for (int i = 0; i < 8; ++i) {
				higherLevelCodes = newHigherLayer(higherLevelCodes);
			}
		}
	}
	
	@Override
	public final int getInputDimension() {
		return this.inputDimension;
	}
	
	public final double[] getHyperplanes() {
		return this.hyperplanes;
	}
	
	public final Collection<BitSet> getClusters() {
		return this.clusters;
	}
	
	public final BitSet encode(final double[] item) {
		return encode(item, this.getHyperplanes());
	}
	
	@Override
	public final boolean accept(final double... item) {
		return this.getClusters().contains(this.encode(item)) ^ this.invertOutput;
	}
	
	@Override
	public final SimpleConfusionMatrix evaluate(final Dataset dataset, final EvaluationMonitor monitor) {
		return Default.defaultEvaluate(this, dataset, monitor);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -6740686339638862795L;
	
	public static final Method CLONE_ELEMENTS = Functional.method(SimplifiedNeuralBinaryClassifier.class, "cloneElements", Collection.class);
	
	@SuppressWarnings("unchecked")
	public static final <T> Collection<T> cloneElements(final Collection<T> elements) {
		try {
			final Collection<T> result = elements.getClass().newInstance();
			
			for (final T element : elements) {
				result.add((T) Functional.CLONE.invoke(element));
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Codeset newHigherLayer(final Codeset codes) {
		final double[] higherLevelData = toData(array(codes.getCodes()[0].keySet(), codes.getCodes()[1].keySet()), codes.getCodeSize());
		final int higherLevelDataStep = codes.getCodeSize() + 1;
		final DoubleList higherLevelHyperplanes = new DoubleList();
		
		generateHyperplanes(higherLevelData, higherLevelDataStep, new HyperplaneHandler() {
			
			@Override
			public final boolean hyperplane(final double bias, final double[] weights) {
				higherLevelHyperplanes.add(bias);
				higherLevelHyperplanes.addAll(weights);
				
				return true;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -4702778886538918117L;
			
		});
		
		final Codeset higherLevelCodes = cluster(higherLevelHyperplanes.toArray(), higherLevelData, higherLevelDataStep);
		
		removeHyperplanes(higherLevelCodes.prune(), higherLevelHyperplanes, higherLevelDataStep);
		
		return higherLevelCodes;
	}
	
	public static final Codeset cluster(final double[] hyperplanes, final double[] data, final int step) {
		debugPrint("Clustering...");
		
		final Codeset result = new Codeset(hyperplanes.length / step);
		final TicToc timer = new TicToc();
		final int dataLength = data.length;
		
		timer.tic();
		
		for (int i = 0; i < dataLength; i += step) {
			if (LOGGING_MILLISECONDS <= timer.toc()) {
				debugPrint(i, "/", dataLength);
				timer.tic();
			}
			
			result.addCode((int) data[i + step - 1], encode(copyOfRange(data, i, i + step - 1), hyperplanes));
		}
		
		debugPrint(result);
		
		return result;
	}
	
	public static final double[] toData(final Collection<BitSet>[] codes, final int codeSize) {
		final int step = codeSize + 1;
		final int n = (codes[0].size() + codes[1].size()) * step;
		final double[] result = new double[n];
		
		for (int labelId = 0, i = 0; labelId < 2; ++labelId) {
			for (final BitSet code : codes[labelId]) {
				for (int bit = 0; bit < codeSize; ++bit) {
					result[i++] = code.get(bit) ? 1.0 : 0.0;
				}
				
				result[i++] = labelId;
			}
		}
		
		return result;
	}
	
	private static final void removeHyperplanes(final BitSet markedHyperplanes, final DoubleList hyperplanes, final int step) {
		final double[] data = hyperplanes.toArray();
		final int n = hyperplanes.size();
		
		for (int i = 0, j = 0, bit = 0; i < n; i += step, ++bit) {
			if (!markedHyperplanes.get(bit)) {
				System.arraycopy(data, i, data, j, step);
				j += step;
			}
		}
		
		hyperplanes.resize(n - markedHyperplanes.cardinality() * step);
	}
	
	public static final BitSet encode(final double[] item, final double[] hyperplanes) {
		final int weightCount = hyperplanes.length;
		final int step = item.length + 1;
		final int hyperplaneCount = weightCount / step;
		final BitSet code = new BitSet(hyperplaneCount);
		
		for (int j = 0, bit = 0; j < weightCount; j += step, ++bit) {
			final double d = hyperplanes[j] + dot(item, copyOfRange(hyperplanes, j + 1, j + step));
			
			code.set(bit, 0.0 <= d);
		}
		
		return code;
	}
	
	public final static void generateHyperplanes(final Dataset trainingData, final HyperplaneHandler hyperplaneHandler) {
		generateHyperplanes(trainingData.getData(), trainingData.getStep(), hyperplaneHandler);
	}
	
	public final static void generateHyperplanes(final double[] data, final int step, final HyperplaneHandler hyperplaneHandler) {
		final int inputDimension = step - 1;
		final int itemCount = data.length / step;
		final List<List<Id>> todo = new ArrayList<List<Id>>();
		final Factory<VectorStatistics> vectorStatisticsFactory = DefaultFactory.forClass(VectorStatistics.class, inputDimension);
		boolean continueProcessing = true;
		final TicToc timer = new TicToc();
		
		timer.tic();
		todo.add(idRange(0, itemCount - 1));
		
		while (!todo.isEmpty() && continueProcessing) {
			if (LOGGING_MILLISECONDS < timer.toc()) {
				debugPrint("remainingRegions:", todo.size());
				timer.tic();
			}
			
			final List<Id> ids = todo.remove(0);
			final VectorStatistics[] statistics = instances(2, vectorStatisticsFactory);
			
			for (final Id id : ids) {
				final int sampleOffset = id.getId() * step;
				final int labelOffset = sampleOffset + step - 1;
				statistics[(int) data[labelOffset]].addValues(copyOfRange(data, sampleOffset, labelOffset));
			}
			
			if (statistics[0].getCount() == 0 || statistics[1].getCount() == 0) {
				continue;
			}
			
			final double[] cluster0 = statistics[0].getMeans();
			final double[] cluster1 = statistics[1].getMeans();
			final double[] neuronWeights = subtract(cluster1, cluster0);
			
			if (Arrays.equals(cluster0, cluster1)) {
				for (int i = 0; i < inputDimension; ++i) {
					neuronWeights[i] = RANDOM.nextDouble();
				}
			}
			
			final int indexCount = ids.size();
			final double[] neuronLocation;
			final int algo = 0;
			
			if (algo == 0) {
				neuronLocation = scaled(add(cluster1, cluster0), 0.5);
			} else {
				for (int i = 0; i < indexCount; ++i) {
					final int sampleOffset = ids.get(i).getId() * step;
					ids.get(i).setSortingKey(dot(neuronWeights, copyOfRange(data, sampleOffset, sampleOffset + inputDimension)));
				}
				
				Collections.sort(ids);
				
				{
					final double actualNegatives = statistics[0].getCount();
					final double actualPositives = statistics[1].getCount();
					final double[] predictedNegatives = new double[2];
					double bestScore = 0.0;
					int bestScoreIndex = 0;
					
					for (int i = 0; i < indexCount; ++i) {
						final int sampleOffset = ids.get(i).getId() * step;
						final int label = (int) data[sampleOffset + inputDimension];
						++predictedNegatives[label];
						final double trueNegatives = predictedNegatives[0];
						final double falseNegatives = predictedNegatives[1];
						final double negatives = trueNegatives + falseNegatives;
						final double truePositives = actualPositives - falseNegatives;
						final double positives = actualPositives + actualNegatives - negatives;
						final double score = algo == 1 ? trueNegatives / negatives + truePositives / positives
								: trueNegatives / actualNegatives + truePositives / actualPositives;
						
						if (bestScore < score) {
							bestScore = score;
							bestScoreIndex = i;
						}
					}
					
					final int i = ids.get(bestScoreIndex).getId() * step;
					final int j = ids.get(bestScoreIndex + 1).getId() * step;
//					neuronLocation = scaled(add(copyOfRange(data, i, i + inputDimension), copyOfRange(data, j, j + inputDimension)), 0.5);
					neuronLocation = add(
							copyOfRange(data, i, i + inputDimension), 15.0 / 16.0,
							copyOfRange(data, j, j + inputDimension), 1.0 / 16.0);
				}
			}
			
			final double neuronBias = -dot(neuronWeights, neuronLocation);
			
			continueProcessing = hyperplaneHandler.hyperplane(neuronBias, neuronWeights);
			
			{
				int j = 0;
				
				for (int i = 0; i < indexCount; ++i) {
					final int sampleOffset = ids.get(i).getId() * step;
					final double d = dot(neuronWeights, copyOfRange(data, sampleOffset, sampleOffset + inputDimension)) + neuronBias;
					
					if (d < 0) {
						swap(ids, i, j++);
					}
				}
				
				if (0 < j && j < indexCount) {
					todo.add(ids.subList(0, j));
					todo.add(ids.subList(j, indexCount));
				}
			}
		}
	}
	
	public static final List<Integer> range(final int first, final int last) {
		final List<Integer> result = new ArrayList<Integer>(last - first + 1);
		
		for (int i = first; i <= last; ++i) {
			result.add(i);
		}
		
		return result;
	}
	
	public static final List<Id> idRange(final int first, final int last) {
		final List<Id> result = new ArrayList<Id>(last - first + 1);
		
		for (int i = first; i <= last; ++i) {
			result.add(new Id(i));
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-03-10)
	 */
	public static abstract interface HyperplaneHandler extends Serializable {
		
		public abstract boolean hyperplane(double bias, double[] weights);
		
	}
	
	/**
	 * @author codistmonk (creation 2014-03-12)
	 */
	public static final class Codeset implements Serializable {
		
		private final Map<BitSet, AtomicInteger>[] codes;
		
		private int codeSize;
		
		@SuppressWarnings("unchecked")
		public Codeset(final int codeSize) {
			this.codes = instances(2, HASH_MAP_FACTORY);
			this.codeSize = codeSize;
		}
		
		public final Map<BitSet, AtomicInteger>[] getCodes() {
			return this.codes;
		}
		
		public final void addCode(final int labelId, final BitSet code) {
			getOrCreate(this.getCodes()[labelId], code, ATOMIC_INTEGER_FACTORY).incrementAndGet();
		}
		
		public final int getCodeSize() {
			return this.codeSize;
		}
		
		public final BitSet prune() {
			debugPrint("Pruning...");
			
			final TicToc timer = new TicToc();
			final int codeSize = this.getCodeSize();
			final BitSet result = new BitSet(codeSize);
			final Collection<BitSet>[] newCodes = array(new HashSet<BitSet>(this.getCodes()[0].keySet()), new HashSet<BitSet>(this.getCodes()[1].keySet()));
			
			timer.tic();
			
			for (int bit = 0; bit < codeSize; ++bit) {
				if (LOGGING_MILLISECONDS <= timer.toc()) {
					debugPrint(bit, "/", codeSize);
					timer.tic();
				}
				
				@SuppressWarnings("unchecked")
				final Set<BitSet>[] simplifiedCodes = instances(2, HASH_SET_FACTORY);
				
				for (int i = 0; i < 2; ++i) {
					for (final BitSet code : newCodes[i]) {
						final BitSet simplifiedCode = (BitSet) code.clone();
						
						simplifiedCode.clear(bit);
						simplifiedCodes[i].add(simplifiedCode);
					}
				}
				
				if (disjoint(simplifiedCodes[0], simplifiedCodes[1])) {
					result.set(bit);
					System.arraycopy(simplifiedCodes, 0, newCodes, 0, 2);
				}
			}
			
			final int newCodeSize = codeSize - result.cardinality(); 
			
			for (int i = 0; i < 2; ++i) {
				this.getCodes()[i].clear();
				
				for (final BitSet newCode : newCodes[i]) {
					final BitSet code = new BitSet(newCodeSize);
					
					for (int oldBit = 0, newBit = 0; oldBit < codeSize; ++oldBit) {
						if (!result.get(oldBit)) {
							code.set(newBit++, newCode.get(oldBit));
						}
					}
					
					this.addCode(i, code);
				}
			}
			
			this.codeSize = newCodeSize;
			
			debugPrint(this);
			
			return result;
		}
		
		@Override
		public final String toString() {
			return "codeSize: " + this.getCodeSize() + " 0-codes: " + this.getCodes()[0].size() + " 1-codes: " + this.getCodes()[1].size();
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6811555918840188741L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-03-21)
	 */
	public static final class Id implements Serializable, Comparable<Id> {
		
		private final int id;
		
		private double sortingKey;
		
		public Id(final int id) {
			this.id = id;
		}
		
		public final double getSortingKey() {
			return this.sortingKey;
		}
		
		public final void setSortingKey(final double sortingKey) {
			this.sortingKey = sortingKey;
		}
		
		public final int getId() {
			return this.id;
		}
		
		@Override
		public final int compareTo(final Id that) {
			return Double.compare(this.getSortingKey(), that.getSortingKey());
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6816291687397666878L;
		
	}
	
}

/**
 * @author codistmonk (creation 2014-03-10)
 */
final class Functional {
	
	private Functional() {
		throw new IllegalInstantiationException();
	}
	
	public static final Method CLONE = method(Object.class, "clone");
	
	@SuppressWarnings("unchecked")
	public static final <In, T> Collection<T>[] map(final In[] methodObjects, final Method method) {
		return map(Collection.class, methodObjects, method);
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, T> Collection<T>[] map(final Object methodObject, final Method method, final In[] singleArguments) {
		return map(Collection.class, methodObject, method, singleArguments);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> Collection<T>[] map(final Object methodObject, final Method method, final Object[][] multipleArguments) {
		return map(Collection.class, methodObject, method, multipleArguments);
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, Out> Out[] map(final Class<Out> resultComponentType,
			final In[] methodObjects, final Method method) {
		try {
			final int n = methodObjects.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObjects[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <In, Out> Out[] map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final In[] singleArguments) {
		try {
			final int n = singleArguments.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObject, singleArguments[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <Out> Out[] map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final Object[][] multipleArguments) {
		try {
			final int n = multipleArguments.length;
			final Out[] result = (Out[]) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				result[i] = (Out) method.invoke(methodObject, multipleArguments[i]);
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Method method(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
		Method result = null;
		
		try {
			result = cls.getMethod(methodName, parameterTypes);
		} catch (final Exception exception) {
			ignore(exception);
		}
		
		if (result == null) {
			try {
				result = cls.getDeclaredMethod(methodName, parameterTypes);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		result.setAccessible(true);
		
		return result;
	}
	
}
