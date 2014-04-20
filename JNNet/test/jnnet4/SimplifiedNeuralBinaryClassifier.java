package jnnet4;

import static java.util.Arrays.copyOfRange;
import static java.util.Collections.disjoint;
import static java.util.Collections.swap;
import static jnnet4.FeedforwardNeuralNetworkTest.intersection;
import static jnnet4.JNNetTools.ATOMIC_INTEGER_FACTORY;
import static jnnet4.JNNetTools.RANDOM;
import static jnnet4.VectorStatistics.add;
import static jnnet4.VectorStatistics.dot;
import static jnnet4.VectorStatistics.subtract;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_MAP_FACTORY;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_SET_FACTORY;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jnnet.DoubleList;

import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public final class SimplifiedNeuralBinaryClassifier implements BinaryClassifier {
	
	private final int inputDimension;
	
	private final double[] hyperplanes;
	
	private final Collection<BitSet> clusters;
	
	private final boolean invertOutput;
	
	public SimplifiedNeuralBinaryClassifier(final Dataset trainingDataset) {
		this(trainingDataset, 0.5, Integer.MAX_VALUE, true, true);
	}
	
	public SimplifiedNeuralBinaryClassifier(final Dataset trainingDataset, final double k, final int maximumHyperplaneCount,
			final boolean allowHyperplanePruning, final boolean allowOutputInversion) {
		debugPrint("Partitioning...");
		
		final DoubleList hyperplanes = new DoubleList();
		final int step = trainingDataset.getItemSize();
		
		generateHyperplanes(trainingDataset, k, new HyperplaneHandler() {
			
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
		
		this.inputDimension = step - 1;
		final Codeset codes = cluster(hyperplanes.toArray(), trainingDataset);
		
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
		this.invertOutput = !codes.getCodes()[0].isEmpty() && (codes.getCodes()[1].isEmpty() ||
				allowOutputInversion && codes.getCodes()[0].size() < codes.getCodes()[1].size());
		this.clusters = new HashSet<BitSet>(this.invertOutput ? codes.getCodes()[0].keySet() : codes.getCodes()[1].keySet());
		
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
		final Dataset higherLevelData = toData(array(codes.getCodes()[0].keySet(), codes.getCodes()[1].keySet()), codes.getCodeSize());
		final int higherLevelDataStep = codes.getCodeSize() + 1;
		final DoubleList higherLevelHyperplanes = new DoubleList();
		
		generateHyperplanes(higherLevelData, 0.5, new HyperplaneHandler() {
			
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
		
		final Codeset higherLevelCodes = cluster(higherLevelHyperplanes.toArray(), higherLevelData);
		
		removeHyperplanes(higherLevelCodes.prune(), higherLevelHyperplanes, higherLevelDataStep);
		
		return higherLevelCodes;
	}
	
	public static final Codeset cluster(final double[] hyperplanes, final Dataset data) {
		debugPrint("Clustering...");
		
		final int step = data.getItemSize() - 1;
		final Codeset result = new Codeset(hyperplanes.length / step);
		final TicToc timer = new TicToc();
		
		timer.tic();
		
		for (int i = 0; i < data.getItemCount(); ++i) {
			if (LOGGING_MILLISECONDS <= timer.toc()) {
				debugPrint(i, "/", data.getItemCount());
				timer.tic();
			}
			
			result.addCode((int) data.getItemLabel(i), encode(data.getItemWeights(i), hyperplanes));
		}
		
		debugPrint(result);
		
		return result;
	}
	
	public static final Dataset toData(final Collection<BitSet>[] codes, final int codeSize) {
		final int itemSize = codeSize + 1;
		final int n = (codes[0].size() + codes[1].size()) * itemSize;
		final double[] data = new double[n];
		
		for (int labelId = 0, i = 0; labelId < 2; ++labelId) {
			for (final BitSet code : codes[labelId]) {
				for (int bit = 0; bit < codeSize; ++bit) {
					data[i++] = code.get(bit) ? 1.0 : 0.0;
				}
				
				data[i++] = labelId;
			}
		}
		
		return new Dataset() {
			
			@Override
			public final int getItemCount() {
				return n / itemSize;
			}
			
			@Override
			public final int getItemSize() {
				return itemSize;
			}
			
			@Override
			public final double getItemValue(final int itemId, final int valueId) {
				return data[itemId * this.getItemSize() + valueId];
			}
			
			@Override
			public final double[] getItem(final int itemId) {
				return copyOfRange(data, itemId * this.getItemSize(), (itemId + 1) * this.getItemSize());
			}
			
			@Override
			public final double[] getItemWeights(final int itemId) {
				return copyOfRange(data, itemId * this.getItemSize(), (itemId + 1) * this.getItemSize() - 1);
			}
			
			@Override
			public final double getItemLabel(final int itemId) {
				return this.getItemValue(itemId, this.getItemSize() - 1);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -7831160202359796099L;
			
		};
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
	
	public static final void generateHyperplanes(final Dataset trainingData, final double k, final HyperplaneHandler hyperplaneHandler) {
		final int inputDimension = trainingData.getItemSize() - 1;
		final int itemCount = trainingData.getItemCount();
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
				statistics[(int) trainingData.getItemLabel(id.getId())].addValues(trainingData.getItemWeights(id.getId()));
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
//				neuronLocation = scaled(add(cluster1, cluster0), 0.5);
				neuronLocation = add(cluster0, k, cluster1, 1.0 - k);
			} else {
				for (int i = 0; i < indexCount; ++i) {
					ids.get(i).setSortingKey(dot(neuronWeights, trainingData.getItemWeights(ids.get(i).getId())));
				}
				
				Collections.sort(ids);
				
				{
					final double actualNegatives = statistics[0].getCount();
					final double actualPositives = statistics[1].getCount();
					final double[] predictedNegatives = new double[2];
					double bestScore = 0.0;
					int bestScoreIndex = 0;
					
					for (int i = 0; i < indexCount; ++i) {
						final int label = (int) trainingData.getItemLabel(ids.get(i).getId());
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
					
					final int i = ids.get(bestScoreIndex).getId();
					final int j = ids.get(bestScoreIndex + 1).getId();
//					neuronLocation = scaled(add(copyOfRange(data, i, i + inputDimension), copyOfRange(data, j, j + inputDimension)), 0.5);
					neuronLocation = add(
							trainingData.getItemWeights(i), k,
							trainingData.getItemWeights(j), 1.0 - k);
				}
			}
			
			final double neuronBias = -dot(neuronWeights, neuronLocation);
			
			continueProcessing = hyperplaneHandler.hyperplane(neuronBias, neuronWeights);
			
			{
				int j = 0;
				
				for (int i = 0; i < indexCount; ++i) {
					final double d = dot(neuronWeights, trainingData.getItemWeights(ids.get(i).getId())) + neuronBias;
					
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
