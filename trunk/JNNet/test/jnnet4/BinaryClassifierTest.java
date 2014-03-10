package jnnet4;

import static java.util.Arrays.copyOfRange;
import static java.util.Collections.swap;
import static jnnet4.JNNetTools.RANDOM;
import static jnnet4.VectorStatistics.add;
import static jnnet4.VectorStatistics.dot;
import static jnnet4.VectorStatistics.scaled;
import static jnnet4.VectorStatistics.subtract;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_SET_FACTORY;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.instances;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jnnet.DoubleList;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class BinaryClassifierTest {
	
	@Test
	public final void test() {
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
		final Dataset trainingData = new Dataset("jnnet/2spirals.txt");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 0, 1000000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
//		debugPrint("Loading test dataset started", new Date(timer.tic()));
//		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 11000000-500000, 500000);
//		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		debugPrint("Building classifier started", new Date(timer.tic()));
		final BinaryClassifier classifier = new BinaryClassifier(trainingData, 2000);
		debugPrint("clusterCount:", classifier.getClusters().size());
		debugPrint("Building classifier done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
		final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData);
		debugPrint("training:", confusionMatrix);
		debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
		
//		debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
//		debugPrint("test:", classifier.evaluate(testData));
//		debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
		
		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
}

/**
 * @author codistmonk (creation 2014-03-10)
 */
final class BinaryClassifier implements Serializable {
	
	private final int step;
	
	private final double[] hyperplanes;
	
	private final Collection<BitSet> clusters;
	
	private final boolean invertOutput;
	
	public BinaryClassifier(final Dataset trainingDataset) {
		this(trainingDataset, Integer.MAX_VALUE);
	}
	
	public BinaryClassifier(final Dataset trainingDataset, final int maximumHyperplaneCount) {
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
		
		final TicToc timer = new TicToc();
		final double[] data = trainingDataset.getData();
		final int dataLength = data.length;
		@SuppressWarnings("unchecked")
		final Collection<BitSet>[] codes = instances(2, HASH_SET_FACTORY);
		this.step = step;
		this.hyperplanes = hyperplanes.toArray();
		
		debugPrint("hyperplaneCount:", this.hyperplanes.length / step);
		debugPrint("Clustering...");
		
		timer.tic();
		
		for (int i = 0; i < dataLength; i += step) {
			if (LOGGING_MILLISECONDS <= timer.toc()) {
				debugPrint(i, "/", dataLength);
				timer.tic();
			}
			
			final double[] item = copyOfRange(data, i, i + step - 1);
			final int label = (int) data[i + step - 1];
			final BitSet code = this.encode(item);
			
			codes[label].add(code);
		}
		
		debugPrint("0-codes:", codes[0].size(), "1-codes:", codes[1].size());
		
		// TODO prune hyperplanes
		
		this.invertOutput = codes[0].size() < codes[1].size();
		this.clusters = this.invertOutput ? codes[0] : codes[1];
	}
	
	public final BitSet encode(final double[] item) {
		final int weightCount = this.hyperplanes.length;
		final int hyperplaneCount = weightCount / this.step;
		final BitSet code = new BitSet(hyperplaneCount);
		
		for (int j = 0, bit = 0; j < weightCount; j += this.step, ++bit) {
			final double d = this.hyperplanes[j] + dot(item, copyOfRange(this.hyperplanes, j + 1, j + this.step));
			
			code.set(bit, 0.0 <= d);
		}
		
		return code;
	}
	
	public final Collection<BitSet> getClusters() {
		return this.clusters;
	}
	
	public final boolean accept(final double... item) {
		return this.getClusters().contains(this.encode(item)) ^ this.invertOutput;
	}
	
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
	private static final long serialVersionUID = -6740686339638862795L;
	
	/**
	 * {@value}.
	 */
	public static final long LOGGING_MILLISECONDS = 5000L;
	
	public final static void generateHyperplanes(final Dataset trainingData, final HyperplaneHandler hyperplaneHandler) {
		final int step = trainingData.getStep();
		final int inputDimension = step - 1;
		final int itemCount = trainingData.getItemCount();
		final double[] data = trainingData.getData();
		final List<List<Integer>> todo = new ArrayList<List<Integer>>();
		final Factory<VectorStatistics> vectorStatisticsFactory = DefaultFactory.forClass(VectorStatistics.class, inputDimension);
		boolean continueProcessing = true;
		final TicToc timer = new TicToc();
		
		timer.tic();
		todo.add(range(0, itemCount - 1));
		
		while (!todo.isEmpty() && continueProcessing) {
			if (LOGGING_MILLISECONDS < timer.toc()) {
				debugPrint("remainingClusters:", todo.size());
				timer.tic();
			}
			
			final List<Integer> indices = todo.remove(0);
			
			final VectorStatistics[] statistics = instances(2, vectorStatisticsFactory);
			
			for (final int i : indices) {
				final int sampleOffset = i * step;
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
			
			final double[] neuronLocation = scaled(add(cluster1, cluster0), 0.5);
			final double neuronBias = -dot(neuronWeights, neuronLocation);
			
			continueProcessing = hyperplaneHandler.hyperplane(neuronBias, neuronWeights);
			
			{
				final int m = indices.size();
				int j = 0;
				
				for (int i = 0; i < m; ++i) {
					final int sampleOffset = indices.get(i) * step;
					final double d = dot(neuronWeights, copyOfRange(data, sampleOffset, sampleOffset + inputDimension)) + neuronBias;
					
					if (0 <= d) {
						swap(indices, i, j++);
					}
				}
				
				if (0 < j && j < m) {
					todo.add(indices.subList(0, j));
					todo.add(indices.subList(j, m));
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
	
	/**
	 * @author codistmonk (creation 2014-03-10)
	 */
	public static abstract interface HyperplaneHandler extends Serializable {
		
		public abstract boolean hyperplane(double bias, double[] weights);
		
	}
	
}
