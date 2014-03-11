package jnnet4;

import static java.util.Arrays.fill;
import static jnnet4.JNNetTools.uint8;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import net.sourceforge.aprog.tools.MathTools.Statistics;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-11)
 */
public final class ProjectiveClassifierTest {
	
	@Test
	public final void test() {
		fail("Not yet implemented");
	}
	
}

/**
 * @author codistmonk (creation 2014-03-11)
 */
final class ProjectiveClassifier implements BinaryClassifier {
	
	private final int inputDimension;
	
	private final BufferedImage image;
	
	private final int thumbnailSize;
	
	public ProjectiveClassifier(final Dataset trainingData, final int thumbnailSize) {
		this.inputDimension = trainingData.getStep() - 1;
		this.image = preview(trainingData, thumbnailSize);
		this.thumbnailSize = thumbnailSize;
	}
	
	@Override
	public final int getInputDimension() {
		return this.inputDimension;
	}
	
	@Override
	public final boolean accept(final double... item) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public final SimpleConfusionMatrix evaluate(final Dataset trainingData) {
		// TODO Auto-generated method stub
		return null;
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
		
		for (int dimensionY = 0; dimensionY < inputDimension; ++dimensionY) {
			final Statistics yStatistics = dataset.getStatistics()[2].getStatistics()[dimensionY];
			final int top = dimensionY * (thumbnailSize + 1);
			
			for (int dimensionX = 0; dimensionX < inputDimension; ++dimensionX) {
				final Statistics xStatistics = dataset.getStatistics()[2].getStatistics()[dimensionX];
				final int left = dimensionX * (thumbnailSize + 1);
				
				for (final int[] labelCounts : counts) {
					fill(labelCounts, 0);
				}
				
				for (int i = 0; i < n; i += step) {
					final double normalizedX = xStatistics.getNormalizedValue(data[i + dimensionX]);
					final double normalizedY = yStatistics.getNormalizedValue(data[i + dimensionY]);
					final int x = (int) ((thumbnailSize - 1) * normalizedX);
					final int y = (int) (thumbnailSize - 1 - (thumbnailSize - 1) * normalizedY);
					final int label = (int) data[i + step - 1];
					++counts[label][y * thumbnailSize + x];
				}
				
				for (int y = top, thumbnailPixel = 0; y < top + thumbnailSize; ++y) {
					for (int x = left; x < left + thumbnailSize; ++x, ++thumbnailPixel) {
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
		}
		
		return result;
	}
	
}
