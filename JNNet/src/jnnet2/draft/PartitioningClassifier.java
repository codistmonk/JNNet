package jnnet2.draft;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
		// TODO Auto-generated constructor stub
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
	
}
