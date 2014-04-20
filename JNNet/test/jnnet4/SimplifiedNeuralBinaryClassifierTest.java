package jnnet4;

import static java.lang.Math.sqrt;
import static jnnet4.JNNetTools.rgb;
import static jnnet4.ProjectiveClassifier.preview;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.writeObject;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.imageio.ImageIO;

import jnnet4.BinaryClassifier.EvaluationMonitor;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class SimplifiedNeuralBinaryClassifierTest {
	
	@Test
	public final void test1() {
		final boolean showClassifier = false;
		final boolean previewTrainingData = false;
		final boolean previewValidationData = false;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final CSVDataset trainingData = new CSVDataset("jnnet/2spirals.txt");
//		final Dataset trainingData = new Dataset("jnnet/iris_versicolor.txt");
//		final CSVDataset trainingData = new CSVDataset("../Libraries/datasets/gisette/gisette_train.data");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 0, 500000);
//		final Dataset trainingData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 0, 500000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (previewTrainingData) {
			SwingTools.show(preview(trainingData, 8), "Training data", false);
		}
		
//		debugPrint("Loading validation dataset started", new Date(timer.tic()));
//		final CSVDataset validationData = new CSVDataset("../Libraries/datasets/gisette/gisette_valid.data");
//		debugPrint("Loading validation dataset done in", timer.toc(), "ms");
//		
//		if (previewValidationData) {
//			SwingTools.show(preview(validationData, 8), "Validation data", false);
//		}
		
//		debugPrint("Loading test dataset started", new Date(timer.tic()));
////		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 11000000-500000, 500000);
////		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 500000, 500000);
////		final Dataset testData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 5000000-500000, 500000);
//		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		for (int maximumHyperplaneCount = 2; maximumHyperplaneCount <= 200; maximumHyperplaneCount += 2) {
			debugPrint("Building classifier started", new Date(timer.tic()));
			final BinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, 0.5, maximumHyperplaneCount, true, true);
			debugPrint("Building classifier done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
			final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData, null);
			debugPrint("training:", confusionMatrix);
			debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
			
//			debugPrint("Evaluating classifier on validation set started", new Date(timer.tic()));
//			debugPrint("test:", classifier.evaluate(validationData, null));
//			debugPrint("Evaluating classifier on validation set done in", timer.toc(), "ms");
			
//			debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
//			debugPrint("test:", classifier.evaluate(testData));
//			debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
			
			if (showClassifier && classifier.getInputDimension() == 2) {
				show(classifier, 256, 16.0, trainingData.getData_());
			}
		}
		
//		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
//	@Test
	public final void test2() throws Exception {
		final boolean showClassifier = true;
		final boolean previewTrainingData = false;
		final TicToc timer = new TicToc();
		final int digit = 4;
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final CSVDataset trainingData = new CSVDataset("../Libraries/datasets/mnist/mnist_" + digit + ".train");
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (false) {
			final double[] data = trainingData.getData_();
			final int n = data.length;
			final int step = trainingData.getItemSize();
			
			new File("mnist/" + digit).mkdirs();
			new File("mnist/not" + digit).mkdirs();
			
			for (int i = 0, k = 500; i < n && 0 < --k; i += step) {
				final int label = (int) data[i + step - 1];
				ImageIO.write(InvertClassifier.newImage(data, i, 28, 28), "png", new File("mnist/" + (label == 0 ? "not" : "") + digit + "/mnist_" + digit + "_training_" + (i / step) + ".png"));
			}
		}
		
		if (previewTrainingData) {
			SwingTools.show(preview(trainingData, 8), "Training data", false);
		}
		
		debugPrint("Loading test dataset started", new Date(timer.tic()));
		final CSVDataset testData = new CSVDataset("../Libraries/datasets/mnist/mnist_" + digit + ".test");
		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		final MNISTErrorMonitor trainingMonitor = new MNISTErrorMonitor(trainingData, 0);
		final MNISTErrorMonitor testMonitor = new MNISTErrorMonitor(testData, 0);
		
		for (int maximumHyperplaneCount = 10; maximumHyperplaneCount <= 10; maximumHyperplaneCount += 2) {
			debugPrint("Building classifier started", new Date(timer.tic()));
			final SimplifiedNeuralBinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, 0.5, maximumHyperplaneCount, true, true);
			debugPrint("Building classifier done in", timer.toc(), "ms");
			
			if (true) {
				final BufferedImage mosaic = InvertClassifier.invert(classifier, 1, 100).generateMosaic();
				
				ImageIO.write(mosaic, "png", new File("mnsit_" + digit + "_mosaic.png"));
				
				SwingTools.show(mosaic, "Cluster representatives", false);
			}
			
			debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
			final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData, trainingMonitor);
			
			debugPrint("training:", confusionMatrix);
			debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
			debugPrint("test:", classifier.evaluate(testData, testMonitor));
			debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
			
			if (showClassifier && classifier.getInputDimension() == 2) {
				show(classifier, 256, 16.0, trainingData.getData_());
			}
		}
		
		{
			show(trainingMonitor, "training");
			show(testMonitor, "test");
			
			gc(10000L);
		}
		
//		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
//	@Test
	public final void test3() throws Exception {
		final TicToc timer = new TicToc();
		final int validationItems = 10000;
		
		debugPrint("Loading full dataset started", new Date(timer.tic()));
		final ReorderingDataset all = new ReorderingDataset(new BinDataset("F:/icpr2014_mitos_atypia/A.bin")).shuffle();
		debugPrint("Loading full dataset done in", timer.toc(), "ms");
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final Dataset trainingData = all.subset(0, all.getItemCount() - validationItems);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		debugPrint("Loading validation dataset started", new Date(timer.tic()));
		final Dataset validationData = all.subset(all.getItemCount() - validationItems, validationItems);
		debugPrint("Loading validation dataset done in", timer.toc(), "ms");
		
		BinaryClassifier bestClassifier = null;
		double bestSensitivity = 0.0;
		
		for (int maximumHyperplaneCount = 38; maximumHyperplaneCount <= 38; maximumHyperplaneCount += 2) {
			debugPrint("Building classifier started", new Date(timer.tic()));
			final SimplifiedNeuralBinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, 0.5, maximumHyperplaneCount, true, true);
			debugPrint("Building classifier done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
			debugPrint("training:", classifier.evaluate(trainingData, null));
			debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
			
			debugPrint("Evaluating classifier on validation set started", new Date(timer.tic()));
			final SimpleConfusionMatrix validationResult = classifier.evaluate(validationData, null);
			debugPrint("validation:", validationResult);
			debugPrint("Evaluating classifier on validation set done in", timer.toc(), "ms");
			
			if (bestSensitivity < validationResult.getSensitivity()) {
				bestSensitivity = validationResult.getSensitivity();
				bestClassifier = classifier;
			}
		}
		
		if (bestClassifier != null) {
			debugPrint("Serializing best classifier started", new Date(timer.tic()));
			writeObject(bestClassifier, "bestclassifier.jo");
			debugPrint("Serializing best classifier done in", timer.toc(), "ms");
		}
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
	
	/**
	 * @author codistmonk (creation 2014-03-25)
	 */
	public static final class MNISTErrorMonitor implements EvaluationMonitor {
		
		private final Collection<BufferedImage> falseNegatives;
		
		private final Collection<BufferedImage> falsePositives;
		
		private final CSVDataset trainingData;
		
		private final int maximumImagesPerCategory;
		
		public MNISTErrorMonitor(final CSVDataset dataset, final int maximumImagesPerCategory) {
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
				final int step = this.trainingData.getItemSize();
				final int w = (int) sqrt(step - 1);
				final int h = w;
				final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				
				for (int y = 0, p = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++p) {
						image.setRGB(x, y, rgb(this.trainingData.getItemValue(sampleId, p) / 255.0));
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
	public static final <In, Out, OutArray> OutArray map(final Class<Out> resultComponentType,
			final Object methodObject, final Method method, final In[] singleArguments) {
		try {
			final int n = singleArguments.length;
			final OutArray result = (OutArray) Array.newInstance(resultComponentType, n);
			
			for (int i = 0; i < n; ++i) {
				Array.set(result, i, method.invoke(methodObject, singleArguments[i]));
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
