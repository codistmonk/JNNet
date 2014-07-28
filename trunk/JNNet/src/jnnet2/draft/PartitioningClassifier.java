package jnnet2.draft;

import static net.sourceforge.aprog.tools.Tools.debugError;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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
		
		this.computeHyperplanes(trainingDataset);
		this.computeClusters(trainingDataset);
		
		// TODO remove redundant hyperplanes
	}
	
	public final BitSet encode(final double... item) {
		final int hyperplaneCount = this.hyperplanes.size();
		final BitSet result = new BitSet(hyperplaneCount);
		
		for (int bit = 0; bit < hyperplaneCount; ++bit) {
			if (0.0 <= hDot(this.hyperplanes.get(bit), item)) {
				result.set(bit);
			}
		}
		
		return result;
	}
	
	@Override
	public final int getInputSize() {
		return this.inputSize;
	}
	
	@Override
	public final double getLabelFor(final double... input) {
		final BitSet cluster = this.encode(input);
		
		for (final Map.Entry<Double, Collection<BitSet>> entry : this.clusters.entrySet()) {
			if (entry.getValue().contains(cluster)) {
				return entry.getKey();
			}
		}
		
		return this.defaultLabel;
	}
	
	private final void computeClusters(final Dataset trainingDataset) {
		final double[] item = new double[trainingDataset.getItemSize()];
		final long itemCount = trainingDataset.getItemCount();
		
		for (long itemId = 0L; itemId < itemCount; ++itemId) {
			trainingDataset.getItem(itemId, item);
			this.clusters.computeIfAbsent(item[this.inputSize], k -> new HashSet<>()).add(this.encode(item));
		}
	}
	
	private final void computeHyperplanes(final Dataset trainingDataset) {
		final List<Subset> todo = new ArrayList<>();
		final double[] item = new double[trainingDataset.getItemSize()];
		
		todo.add(new Subset(trainingDataset, Subset.idRange(trainingDataset.getItemCount())));
		
		while (!todo.isEmpty()) {
			final Subset subset = todo.remove(0);
			final double[] hyperplane = subset.getHyperplane();
			final int labelOffset = this.inputSize;
			
			this.hyperplanes.add(hyperplane);
			
			final LongList below = new LongList();
			final BitSet belowClasses = new BitSet();
			final LongList above = new LongList();
			final BitSet aboveClasses = new BitSet();
			
			subset.getItemIds().forEach(new Processor() {
				
				@Override
				public final boolean process(final long itemId) {
					if (hDot(hyperplane, trainingDataset.getItem(itemId, item)) < 0.0) {
						below.add(itemId);
						belowClasses.set(trainingDataset.getLabelStatistics().getLabelId(item[labelOffset]));
					} else {
						above.add(itemId);
						aboveClasses.set(trainingDataset.getLabelStatistics().getLabelId(item[labelOffset]));
					}
					
					return true;
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -7017451880265708569L;
				
			});
			
			if (2 <= belowClasses.cardinality()) {
				todo.add(new Subset(trainingDataset, below));
			}
			
			if (2 <= aboveClasses.cardinality()) {
				todo.add(new Subset(trainingDataset, above));
			}
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 3548531675332877719L;
	
	public static final double hDot(final double[] hyperplane, final double[] input) {
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
			
			if (!isFinite(this.centersCovarianceMatrix)) {
				debugError(Arrays.toString(this.centersCovarianceMatrix));
				throw new IllegalStateException();
			}
		}
		
		public final LongList getItemIds() {
			return this.itemIds;
		}
		
		public static final boolean isFinite(final double... array) {
			for (final double value : array) {
				if (!Double.isFinite(value)) {
					return false;
				}
			}
			
			return true;
		}
		
		public final double[] getHyperplane() {
			this.centersCovarianceBasicMatrix = LDATest.matrix(this.dimension, this.centersCovarianceMatrix);
			this.covarianceBasicMatrix = LDATest.matrix(this.dimension, this.covarianceMatrix);
			
			try {
				final BasicMatrix principalComponent = computePrincipalComponent(this.centersCovarianceBasicMatrix);
				final BasicMatrix hyperplaneDirection = this.covarianceBasicMatrix.solve(principalComponent);
				final double[] result = new double[this.dimension + 1];
				
				for (int i = 0; i < this.dimension; ++i) {
					result[1 + i] = hyperplaneDirection.doubleValue(i);
				}
				
				{
					final double[] center = this.computeCenter();
					
					result[0] = -LinearConstraintSystem.Abstract.dot(center, 0, result, 1, this.dimension);
				}
				
				return result;
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		private final double[] computeCenter() {
			final double[] center = new double[this.dimension];
			int classCount = 0;
			
			for (final double count : this.counts) {
				if (0.0 < count) {
					++classCount;
				}
			}
			
			for (final double[] classCenter : this.centers) {
				LinearConstraintSystem.Abstract.add(1.0, classCenter, 0, 1.0, center, 0, center, 0, this.dimension);
			}
			
			for (int i = 0; i < this.dimension; ++i) {
				center[i] /= classCount;
			}
			return center;
		}
		
		private final void readDataset(final Dataset dataset) {
			final int n = this.dimension;
			final double[] item = new double[dataset.getItemSize()];
			final double[] covarianceMatrix = this.covarianceMatrix;
			final double[][] centers = this.centers;
			final double[] counts = this.counts;
			
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
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 8636877199939323311L;
				
			});
		}
		
		private final void updateCovarianceMatrices() {
			final int n = this.dimension;
			
			{
				final int centerCount = this.centers.length;
				
				for (int i = 0; i < centerCount; ++i) {
					final double[] center = this.centers[i];
					final double count = this.counts[i];
					
					if (0.0 < count) {
						for (int j = 0; j < n; ++j) {
							center[j] /= count;
						}
						
						updateCovarianceMatrixUpperHalf(this.centersCovarianceMatrix, center, n);
					}
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
			
			if (true) {
				for (int i = 0; i < n; ++i) {
					this.centersCovarianceMatrix[i * n + i] += 1.0E-9;
					this.covarianceMatrix[i * n + i] += 1.0E-9;
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
		
		public static final BasicMatrix computePrincipalComponent(final BasicMatrix covarianceMatrix) {
			try {
				final int n = covarianceMatrix.getRowDim();
				final Method getComputedEigenvalue = covarianceMatrix.getClass().getSuperclass().getDeclaredMethod("getComputedEigenvalue");
				getComputedEigenvalue.setAccessible(true);
				@SuppressWarnings("unchecked")
				final Eigenvalue<Double> eigenvalue = (Eigenvalue<Double>) getComputedEigenvalue.invoke(covarianceMatrix);
				final MatrixStore<Double> eigenvalues = eigenvalue.getD();
				final MatrixStore<Double> eigenvectors = eigenvalue.getV();
				final MatrixBuilder<Double> builder = PrimitiveMatrix.getBuilder(n);
				int principalComponentIndex = 0;
				
				for (int i = 1; i < n; ++i) {
					if (eigenvalues.get(principalComponentIndex, principalComponentIndex) < eigenvalues.get(i, i)) {
						principalComponentIndex = i;
					}
				}
				
				for (int i = 0; i < n; ++i) {
					builder.set(i, eigenvectors.get(i, principalComponentIndex));
				}
				
				return builder.build();
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
	}
	
}
