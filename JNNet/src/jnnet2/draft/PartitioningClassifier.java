package jnnet2.draft;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.aprog.tools.Tools;
import nsphere.LDATest;

import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixBuilder;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.MatrixStore;

import jgencode.primitivelists.LongList;
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
		
		todo.add(new Subset(trainingDataset));
		
		// TODO
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
		
		public Subset(final Dataset dataset) {
			final int n = dataset.getItemSize() - 1;
			final int classCount = dataset.getLabelStatistics().getLabelCount();
			this.dimension = n;
			this.centersCovarianceMatrix = new double[n * n];
			this.covarianceMatrix = this.centersCovarianceMatrix.clone();
			this.itemIds = new LongList();
			this.centers = new double[classCount][n];
			this.counts = new double[classCount];
			
			long itemId = -1L;
			
			for (final double[] item : dataset) {
				updateCovarianceMatrixUpperHalf(this.covarianceMatrix, item, n);
				
				this.itemIds.add(++itemId);
				
				final int labelId = dataset.getLabelStatistics().getLabelId(item[n]);
				
				LinearConstraintSystem.Abstract.add(1.0, item, 0, 1.0, this.centers[labelId], 0
						, this.centers[labelId], 0, n);
				++this.counts[labelId];
			}
			
			copyUpperHalfToLowerHalf(this.covarianceMatrix, n);
		}
		
		public final double[] getHyperplane() {
			{
				final int n = this.centers.length;
				
				for (int i = 0; i < n; ++i) {
					final double[] center = this.centers[i];
					final double count = this.counts[i];
					
					for (int j = 0; j < this.dimension; ++j) {
						center[j] /= count;
					}
					
					updateCovarianceMatrixUpperHalf(this.centersCovarianceMatrix, center, this.dimension);
				}
				
				copyUpperHalfToLowerHalf(this.centersCovarianceMatrix, this.dimension);
			}
			
			this.centersCovarianceBasicMatrix = LDATest.matrix(this.dimension, this.centersCovarianceMatrix);
			this.covarianceBasicMatrix = LDATest.matrix(this.dimension, this.covarianceMatrix);
			
			try {
				final Method getComputedEigenvalue = this.centersCovarianceBasicMatrix.getClass().getSuperclass().getDeclaredMethod("getComputedEigenvalue");
				getComputedEigenvalue.setAccessible(true);
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
				
				// TODO compute offset
				
				Tools.debugPrint(Arrays.toString(result));
				
				return result;
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6201954999928411597L;
		
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
		
	}
	
}
