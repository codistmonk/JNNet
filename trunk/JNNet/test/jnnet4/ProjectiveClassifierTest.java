package jnnet4;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.fill;
import static jnnet4.JNNetTools.uint8;
import static jnnet4.ProjectiveClassifier.preview;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Date;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.MathTools.Statistics;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-11)
 */
public final class ProjectiveClassifierTest {
	
	@Test
	public final void test() {
		final boolean showClassifier = true;
		final boolean previewTrainingData = true;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final Dataset trainingData = new Dataset("jnnet/2spirals.txt");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/gisette/gisette_train.data");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 0, 500000);
//		final Dataset trainingData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 0, 500000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (previewTrainingData) {
			SwingTools.show(preview(trainingData, 64), "Training data", false);
		}
		
		debugPrint("Building classifier started", new Date(timer.tic()));
		final BinaryClassifier classifier = new ProjectiveClassifier(trainingData, 64);
		debugPrint("Building classifier done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
		final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData);
		debugPrint("training:", confusionMatrix);
		debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
		
		if (showClassifier && classifier.getInputDimension() == 2) {
			SimplifiedNeuralBinaryClassifierTest.show(classifier, 256, 16.0, trainingData.getData());
		}
		
		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
}

/**
 * @author codistmonk (creation 2014-03-11)
 */
final class ProjectiveClassifier implements BinaryClassifier {
	
	private final int inputDimension;
	
	private final VectorStatistics[] statistics;
	
	private final BufferedImage image;
	
	private final int thumbnailSize;
	
	public ProjectiveClassifier(final Dataset trainingData, final int thumbnailSize) {
		this.inputDimension = trainingData.getStep() - 1;
		this.statistics = trainingData.getStatistics();
		this.image = preview(trainingData, thumbnailSize);
		this.thumbnailSize = thumbnailSize;
	}
	
	@Override
	public final int getInputDimension() {
		return this.inputDimension;
	}
	
	@Override
	public final boolean accept(final double... item) {
		final BufferedImage image = this.image;
		final int thumbnailSize = this.thumbnailSize;
		final VectorStatistics[] statistics = this.statistics;
		final double[] maximumProbability = { 0.0 };
		final int[] label = { 0 };
		
		forEachThumbnail(this.getInputDimension(), thumbnailSize, new ThumbnailProcessor() {
			
			@Override
			public final void thumbnail(final int dimensionIndexOfX, final int dimensionIndexOfY,
					final int thumbnailLeft, final int thumbnailTop) {
				final Statistics xStatistics = statistics[2].getStatistics()[dimensionIndexOfX];
				final Statistics yStatistics = statistics[2].getStatistics()[dimensionIndexOfY];
				
				final double normalizedX = clamp01(xStatistics.getNormalizedValue(item[dimensionIndexOfX]));
				final double normalizedY = clamp01(yStatistics.getNormalizedValue(item[dimensionIndexOfY]));
				final int x = (int) ((thumbnailSize - 1) * normalizedX);
				final int y = (int) (thumbnailSize - 1 - (thumbnailSize - 1) * normalizedY);
				try {
					final int rgb = image.getRGB(thumbnailLeft + x, thumbnailTop + y);
					final int red = (rgb >> 16) & 0xFF;
					final int green = (rgb >> 8) & 0xFF;
					
					if (maximumProbability[0] < red) {
						maximumProbability[0] = red;
						label[0] = 0;
					}
					
					if (maximumProbability[0] < green) {
						maximumProbability[0] = green;
						label[0] = 1;
					}
				} catch (final Exception exception) {
					debugPrint(normalizedX, normalizedY, x, y, thumbnailLeft, thumbnailTop);
					throw unchecked(exception);
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -7224924379324858766L;
			
		});
		
		return label[0] != 0;
	}
	
	@Override
	public final SimpleConfusionMatrix evaluate(final Dataset trainingData) {
		final TicToc timer = new TicToc();
		final SimpleConfusionMatrix result = new SimpleConfusionMatrix();
		final int step = trainingData.getStep();
		final double[] data = trainingData.getData();
		
		timer.tic();
		
		for (int i = 0; i < data.length; i += step) {
			if (LOGGING_MILLISECONDS <= timer.toc()) {
				debugPrint(i, "/", data.length);
				timer.tic();
			}
			
			final double expected = data[i + step - 1];
			final double actual = this.accept(copyOfRange(data, i, i + step - 1)) ? 1.0 : 0.0;
			
			if (expected != actual) {
				if (expected == 1.0) {
					result.getFalseNegativeCount().incrementAndGet();
				} else {
					result.getFalsePositiveCount().incrementAndGet();
				}
			} else {
				if (expected == 1.0) {
					result.getTruePositiveCount().incrementAndGet();
				} else {
					result.getTrueNegativeCount().incrementAndGet();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -6644108346388220165L;
	
	public static final double clamp01(final double value) {
		return max(0.0, min(value, 1.0));
	}
	
	public static final void forEachThumbnail(final int inputDimension, final int thumbnailSize, final ThumbnailProcessor processor) {
		for (int dimensionY = 0; dimensionY < inputDimension; ++dimensionY) {
			final int top = dimensionY * (thumbnailSize + 1);
			
			for (int dimensionX = 0; dimensionX < inputDimension; ++dimensionX) {
				final int left = dimensionX * (thumbnailSize + 1);
				
				processor.thumbnail(dimensionX, dimensionY, left, top);
			}
		}
	}
	
	public static final BufferedImage preview(final Dataset dataset, final int thumbnailSize) {
		final int step = dataset.getStep();
		final double[] data = dataset.getData();
		final int n = data.length;
		final int inputDimension = dataset.getStep() - 1;
		final int w = inputDimension * (thumbnailSize + 1) - 1;
		final int h = w;
		final BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		final int[][] counts = new int[2][thumbnailSize * thumbnailSize];
		
		forEachThumbnail(inputDimension, thumbnailSize, new ThumbnailProcessor() {
			
			@Override
			public final void thumbnail(final int dimensionIndexOfX, final int dimensionIndexOfY,
					final int thumbnailLeft, final int thumbnailTop) {
				for (final int[] labelCounts : counts) {
					fill(labelCounts, 0);
				}
				
				final Statistics xStatistics = dataset.getStatistics()[2].getStatistics()[dimensionIndexOfX];
				final Statistics yStatistics = dataset.getStatistics()[2].getStatistics()[dimensionIndexOfY];
				
				for (int i = 0; i < n; i += step) {
					final double normalizedX = xStatistics.getNormalizedValue(data[i + dimensionIndexOfX]);
					final double normalizedY = yStatistics.getNormalizedValue(data[i + dimensionIndexOfY]);
					final int x = (int) ((thumbnailSize - 1) * normalizedX);
					final int y = (int) (thumbnailSize - 1 - (thumbnailSize - 1) * normalizedY);
					final int label = (int) data[i + step - 1];
					++counts[label][y * thumbnailSize + x];
				}
				
				for (int y = thumbnailTop, thumbnailPixel = 0; y < thumbnailTop + thumbnailSize; ++y) {
					for (int x = thumbnailLeft; x < thumbnailLeft + thumbnailSize; ++x, ++thumbnailPixel) {
						final int negativeCounts = counts[0][thumbnailPixel];
						final int positiveCounts = counts[1][thumbnailPixel];
						final int sum = negativeCounts + positiveCounts;
						
						if (0 < sum) {
							final int red = uint8((double) negativeCounts / sum);
							final int green = uint8((double) positiveCounts / sum);
							
							result.setRGB(x, y, 0xFF000000 | (red << 16) | (green << 8));
						}
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 5062047123625280261L;
			
		});
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-03-11)
	 */
	public static abstract interface ThumbnailProcessor extends Serializable {
		
		public abstract void thumbnail(int dimensionIndexOfX, int dimensionIndexOfY,
				int thumbnailLeft, int thumbnailTop);
		
	}
	
}
