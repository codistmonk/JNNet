package jnnet;

import static java.lang.Math.sqrt;
import static jnnet.JNNetTools.rgb;
import static jnnet.draft.ProjectiveClassifier.preview;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.writeObject;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import jnnet.BinDataset;
import jnnet.BinaryClassifier;
import jnnet.CSVDataset;
import jnnet.Dataset;
import jnnet.JNNetTools;
import jnnet.ReorderingDataset;
import jnnet.SimpleConfusionMatrix;
import jnnet.SimplifiedNeuralBinaryClassifier;
import jnnet.BinaryClassifier.EvaluationMonitor;
import jnnet.draft.InvertClassifier;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class SimplifiedNeuralBinaryClassifierTest {
	
//	@Test
	public final void test1() {
		final boolean showClassifier = true;
		final boolean previewTrainingData = false;
		final boolean previewValidationData = false;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final CSVDataset trainingData = new CSVDataset("jnnet/data/2spirals.txt");
//		final Dataset trainingData = new Dataset("jnnet/data/iris_versicolor.txt");
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
		
		for (int maximumHyperplaneCount = 200; maximumHyperplaneCount <= 200; maximumHyperplaneCount += 2) {
			debugPrint("Building classifier started", new Date(timer.tic()));
			final BinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, 0.5, 0.08, maximumHyperplaneCount, true, true);
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
			final SimplifiedNeuralBinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(trainingData, 0.5, 0.08, maximumHyperplaneCount, true, true);
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
	
	@Test
	public final void test3() throws Exception {
		final TicToc timer = new TicToc();
		final int testItems = 10000;
		final int crossValidationFolds = 10;
		
		debugPrint("Loading full dataset started", new Date(timer.tic()));
		final ReorderingDataset all = new ReorderingDataset(new BinDataset("F:/icpr2014_mitos_atypia/A.bin")).shuffle();
		debugPrint("Loading full dataset done in", timer.toc(), "ms");
		
		debugPrint("Loading test dataset started", new Date(timer.tic()));
		final Dataset testData = all.subset(all.getItemCount() - testItems, testItems);
		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		debugPrint("Loading full training dataset started", new Date(timer.tic()));
		final ReorderingDataset fullTrainingData = all.subset(0, all.getItemCount() - testItems);
		debugPrint("Loading full training dataset done in", timer.toc(), "ms");
		
		final int validationItems = crossValidationFolds == 1 ? 0
				: (all.getItemCount() - testItems) / crossValidationFolds;
		
		final List<Dataset[]> trainingValidationPairs = new ArrayList<>(crossValidationFolds);
		
		for (int fold = 1; fold <= crossValidationFolds; ++fold) {
			fullTrainingData.swapFolds(0, fold - 1, crossValidationFolds);
			
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading training dataset started", new Date(timer.tic()));
			final Dataset trainingData = fullTrainingData.subset(validationItems, fullTrainingData.getItemCount() - validationItems);
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading training dataset done in", timer.toc(), "ms");
			
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading validation dataset started", new Date(timer.tic()));
			final Dataset validationData = validationItems == 0 ? trainingData : fullTrainingData.subset(0, validationItems);
			debugPrint("fold:", fold + "/" + crossValidationFolds, "Loading validation dataset done in", timer.toc(), "ms");
			
			trainingValidationPairs.add(array(trainingData, validationData));
			
			fullTrainingData.swapFolds(0, fold - 1, crossValidationFolds);
		}
		
		int bestClassifierParameter = 0;
		double bestSensitivity = 0.0;
		
		for (int classifierParameter = 40; 0 <= classifierParameter; classifierParameter -= 2) {
			debugPrint("classifierParameter:", classifierParameter);
			
			final Statistics sensitivity = new Statistics();
			int fold = 1;
			
			for (final Dataset[] trainingValidationPair : trainingValidationPairs) {
				final Dataset trainingData = trainingValidationPair[0];
				final Dataset validationData = trainingValidationPair[1];
				
				debugPrint("fold:", fold + "/" + crossValidationFolds, "Building classifier started", new Date(timer.tic()));
				final SimplifiedNeuralBinaryClassifier classifier = new SimplifiedNeuralBinaryClassifier(
						trainingData, 0.5, classifierParameter / 100.0, 100, true, true);
				debugPrint("fold:", fold + "/" + crossValidationFolds, "Building classifier done in", timer.toc(), "ms");
				
				debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on training set started", new Date(timer.tic()));
				debugPrint("fold:", fold + "/" + crossValidationFolds, "training:", classifier.evaluate(trainingData, null));
				debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on training set done in", timer.toc(), "ms");
				
				debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on validation set started", new Date(timer.tic()));
				final SimpleConfusionMatrix validationResult = classifier.evaluate(validationData, null);
				debugPrint("fold:", fold + "/" + crossValidationFolds, "validation:", validationResult);
				debugPrint("fold:", fold + "/" + crossValidationFolds, "Evaluating classifier on validation set done in", timer.toc(), "ms");
				
				sensitivity.addValue(validationResult.getSensitivity());
				++fold;
			}
			
			debugPrint("classifierParameter:", classifierParameter, "sensitivity:", sensitivity.getMinimum() + "<=" + sensitivity.getMean() + "(" + sqrt(sensitivity.getVariance()) + ")<=" + sensitivity.getMaximum());
			
			if (bestSensitivity < sensitivity.getMean()) {
				bestSensitivity = sensitivity.getMean();
				bestClassifierParameter = classifierParameter;
			}
		}
		
		if (bestClassifierParameter != 0) {
			debugPrint("bestClassifierParameter:", bestClassifierParameter);
			
			debugPrint("Building best classifier started", new Date(timer.tic()));
			final SimplifiedNeuralBinaryClassifier bestClassifier = new SimplifiedNeuralBinaryClassifier(
					fullTrainingData, 0.5, bestClassifierParameter / 100.0, 100, true, true);
			debugPrint("Building best classifier done in", timer.toc(), "ms");
			
			debugPrint("Evaluating best classifier on training set started", new Date(timer.tic()));
			debugPrint("training:", bestClassifier.evaluate(fullTrainingData, null));
			debugPrint("Evaluating best classifier on training set done in", timer.toc(), "ms");
			
			debugPrint("Evaluating best classifier on test set started", new Date(timer.tic()));
			final SimpleConfusionMatrix testResult = bestClassifier.evaluate(testData, null);
			debugPrint("validation:", testResult);
			debugPrint("Evaluating best classifier on test set done in", timer.toc(), "ms");
			
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
