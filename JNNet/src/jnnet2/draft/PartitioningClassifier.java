package jnnet2.draft;

import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_SET_FACTORY;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.Tools;
import nsphere.LDATest;

import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.MatrixStore;

import jgencode.primitivelists.LongList;
import jgencode.primitivelists.LongList.Processor;
import jnnet.draft.LinearConstraintSystem;
import jnnet2.core.Classifier;
import jnnet2.core.Dataset;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class PartitioningClassifier implements Classifier {
	
	private final List<double[]> hyperplanes;
	
	private final Map<Double, Collection<BitSet>> clusters;
	
	private final int inputSize;
	
	private final double defaultLabel;
	
	public PartitioningClassifier(final Dataset trainingDataset) {
		this.hyperplanes = new ArrayList<>();
		this.clusters = new TreeMap<>();
		this.inputSize = trainingDataset.getItemSize() - 1;
		this.defaultLabel = Double.NaN;
		
		final List<Subset> todo = new ArrayList<>();
		final double[] item = new double[trainingDataset.getItemSize()];
		
		todo.add(new Subset(trainingDataset, Subset.idRange(trainingDataset.getItemCount())));
		
		while (!todo.isEmpty()) {
			final Subset subset = todo.remove(0);
			final double[] hyperplane = subset.getHyperplane();
			
			this.hyperplanes.add(hyperplane);
			
			final LongList below = new LongList();
			final BitSet belowClasses = new BitSet();
			final LongList above = new LongList();
			final BitSet aboveClasses = new BitSet();
			
			subset.getItemIds().forEach(new Processor() {
				
				@Override
				public final boolean process(final long itemId) {
					if (evaluate(hyperplane, trainingDataset.getItem(itemId, item)) < 0.0) {
						below.add(itemId);
						belowClasses.set(trainingDataset.getLabelStatistics().getLabelId(item[inputSize]));
					} else {
						above.add(itemId);
						aboveClasses.set(trainingDataset.getLabelStatistics().getLabelId(item[inputSize]));
					}
					
					return true;
				}
				
			});
			
			if (2 <= belowClasses.cardinality()) {
				todo.add(new Subset(trainingDataset, below));
			}
			
			if (2 <= aboveClasses.cardinality()) {
				todo.add(new Subset(trainingDataset, above));
			}
		}
		
		
		{
			final int hyperplaneCount = this.hyperplanes.size();
			final long itemCount = trainingDataset.getItemCount();
			
			Tools.debugPrint(hyperplaneCount);
			
			for (long itemId = 0L; itemId < itemCount; ++itemId) {
				trainingDataset.getItem(itemId, item);
				getOrCreate(this.clusters, item[this.inputSize], (Factory) HASH_SET_FACTORY).add(this.encode(item));
			}
		}
	}
	
	public final BitSet encode(final double[] item) {
		final int hyperplaneCount = this.hyperplanes.size();
		final BitSet result = new BitSet(hyperplaneCount);
		
		for (int bit = 0; bit < hyperplaneCount; ++bit) {
			if (0.0 <= evaluate(this.hyperplanes.get(bit), item)) {
				result.set(bit);
			}
		}
		
		return result;
	}
	
	public static final double evaluate(final double[] hyperplane, final double[] item) {
		double result = hyperplane[0];
		final int n = hyperplane.length;
		
		for (int i = 1; i < n; ++i) {
			result += hyperplane[i] * item[i - 1];
		}
		
		return result;
	}
	
	@Override
	public final int getInputSize() {
		return this.inputSize;
	}
	
	@Override
	public final double getLabelFor(final double... input) {
		final int n = this.hyperplanes.size();
		final BitSet cluster = new BitSet(n);
		
		for (int bit = 0; bit < n; ++bit) {
			if (0 <= dotH(this.hyperplanes.get(bit), input)) {
				cluster.set(bit);
			}
		}
		
		for (final Map.Entry<Double, Collection<BitSet>> entry : this.clusters.entrySet()) {
			if (entry.getValue().contains(cluster)) {
				return entry.getKey();
			}
		}
		
		return this.defaultLabel;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 3548531675332877719L;
	
	public static final double dotH(final double[] hyperplane, final double[] input) {
		final int n = hyperplane.length;
		double result = hyperplane[0];
		
		for (int i = 1; i < n; ++i) {
			result += hyperplane[i] * input[i - 1];
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-07-12)
	 */
	public static final class Subset implements Serializable {
		
		private final int dimension;
		
		private final double[] centersCovarianceMatrix;
		
		private final double[] covarianceMatrix;
		
		private final LongList itemIds;
		
		private final double[][] centers;
		
		private final double[] counts;
		
		private BasicMatrix centersCovarianceBasicMatrix;
		
		private BasicMatrix covarianceBasicMatrix;
		
		public Subset(final Dataset dataset, final LongList itemIds) {
			final int n = dataset.getItemSize() - 1;
			final int classCount = dataset.getLabelStatistics().getLabelCount();
			this.dimension = n;
			this.centersCovarianceMatrix = new double[n * n];
			this.covarianceMatrix = this.centersCovarianceMatrix.clone();
			this.itemIds = itemIds;
			this.centers = new double[classCount][n];
			this.counts = new double[classCount];
			
			this.readDataset(dataset);
			this.updateCovarianceMatrices();
		}
		
		public final LongList getItemIds() {
			return this.itemIds;
		}
		
		public final double[] getHyperplane() {
			this.centersCovarianceBasicMatrix = LDATest.matrix(this.dimension, this.centersCovarianceMatrix);
			this.covarianceBasicMatrix = LDATest.matrix(this.dimension, this.covarianceMatrix);
			
			Tools.debugPrint(this.centersCovarianceBasicMatrix);
			Tools.debugPrint(this.covarianceBasicMatrix);
			
			try {
				final Method getComputedEigenvalue = this.centersCovarianceBasicMatrix.getClass().getSuperclass().getDeclaredMethod("getComputedEigenvalue");
				getComputedEigenvalue.setAccessible(true);
				@SuppressWarnings("unchecked")
				final Eigenvalue<Double> eigenvalue = (Eigenvalue<Double>) getComputedEigenvalue.invoke(this.centersCovarianceBasicMatrix);
				final MatrixStore<Double> eigenvectors = eigenvalue.getV();
				final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(this.dimension);
				
				for (int i = 0; i < this.dimension; ++i) {
					builder.set(i, eigenvectors.get(i, 0));
				}
				
				final BasicMatrix hyperplaneDirection = this.covarianceBasicMatrix.solve(builder.build());
				final double[] result = new double[this.dimension + 1];
				
				for (int i = 0; i < this.dimension; ++i) {
					result[1 + i] = hyperplaneDirection.doubleValue(i);
				}
				
				{
					final double[] center = new double[this.dimension];
					final int classCount = this.centers.length;
					
					for (final double[] classCenter : this.centers) {
						LinearConstraintSystem.Abstract.add(1.0, classCenter, 0, 1.0, center, 0, center, 0, this.dimension);
					}
					
					for (int i = 0; i < this.dimension; ++i) {
						center[i] /= classCount;
					}
					
					result[0] = -LinearConstraintSystem.Abstract.dot(center, 0, result, 1, this.dimension);
				}
				
				Tools.debugPrint(Arrays.toString(result));
				
				return result;
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		private final void readDataset(final Dataset dataset) {
			final int n = this.dimension;
			final double[] item = new double[dataset.getItemSize()];
			
			this.itemIds.forEach(new Processor() {
				
				@Override
				public final boolean process(final long itemId) {
					dataset.getItem(itemId, item);
					
					updateCovarianceMatrixUpperHalf(covarianceMatrix, item, n);
					
					final int labelId = dataset.getLabelStatistics().getLabelId(item[n]);
					
					LinearConstraintSystem.Abstract.add(1.0, item, 0, 1.0, centers[labelId], 0
							, centers[labelId], 0, n);
					++counts[labelId];
					
					return true;
				}
				
			});
		}
		
		private final void updateCovarianceMatrices() {
			final int n = this.dimension;
			
			{
				final int centerCount = this.centers.length;
				
				for (int i = 0; i < centerCount; ++i) {
					final double[] center = this.centers[i];
					final double count = this.counts[i];
					
					for (int j = 0; j < n; ++j) {
						center[j] /= count;
					}
					
					updateCovarianceMatrixUpperHalf(this.centersCovarianceMatrix, center, n);
				}
				
				copyUpperHalfToLowerHalf(this.centersCovarianceMatrix, n);
			}
			
			{
				for (int i = 0; i < n; ++i) {
					for (int j = i; j < n; ++j) {
						final int k = i * n + j;
						this.covarianceMatrix[k] -= this.centersCovarianceMatrix[k];
					}
				}
				
				copyUpperHalfToLowerHalf(this.covarianceMatrix, n);
			}
			
			{
				final int n2 = this.centersCovarianceMatrix.length;
				
				for (int i = 0; i < n2; ++i) {
					this.centersCovarianceMatrix[i] += RANDOM.nextDouble() * 1.0E-9;
					this.covarianceMatrix[i] += RANDOM.nextDouble() * 1.0E-9;
				}
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6201954999928411597L;
		
		public static final Random RANDOM = new Random(0L);
		
		public static final void updateCovarianceMatrixUpperHalf(final double[] covarianceMatrix
				, final double[] item, final int dimension) {
			for (int i = 0; i < dimension; ++i) {
				for (int j = i; j < dimension; ++j) {
					final double d = item[i] * item[j];
					covarianceMatrix[i * dimension + j] += d;
				}
			}
		}
		
		public static final void copyUpperHalfToLowerHalf(final double[] matrix, final int dimension) {
			for (int i = 0; i < dimension; ++i) {
				for (int j = i + 1; j < dimension; ++j) {
					matrix[j * dimension + i] = matrix[i * dimension + j];
				}
			}
		}
		
		public static final LongList idRange(final long itemCount) {
			final LongList result = new LongList((int) itemCount);
			
			for (long id = 0L; id < itemCount; ++id) {
				result.add(id);
			}
			
			return result;
		}
		
	}
	
}
