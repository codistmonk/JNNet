package jnnet2.core;

import static multij.tools.Tools.getOrCreate;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import multij.tools.Factory;
import multij.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2014-07-08)
 */
public final class ConfusionMatrix implements Serializable {
	
	private final Map<Double, Map<Double, double[]>> data = new TreeMap<>();
	
	@SuppressWarnings("unchecked")
	public final ConfusionMatrix count(final double predictedLabel, final double referenceLabel) {
		++getOrCreate((Map<Double, double[]>) getOrCreate(this.data, predictedLabel, (Factory) DefaultFactory.TREE_MAP_FACTORY)
				, referenceLabel, INDIRECT_DOUBLE_FACTORY)[0];
		
		return this;
	}
	
	public final double getCount(final double predictedLabel, final double referenceLabel) {
		final Map<Double, double[]> row = this.data.get(predictedLabel);
		
		if (row == null) {
			return 0.0;
		}
		
		final double[] cell = row.get(referenceLabel);
		
		return cell == null ? 0.0 : cell[0];
	}
	
	public final ConfusionMatrix updateWith(final Classifier classifier, final Iterable<double[]> items) {
		final int n = classifier.getInputSize();
		
		for (final double[] item : items) {
			this.count(classifier.getLabelFor(item), item[n]);
		}
		
		return this;
	}
	
	@Override
	public final String toString() {
		return this.data.toString();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 4979284306641656497L;
	
	public static final Factory<double[]> INDIRECT_DOUBLE_FACTORY = new Factory<double[]>() {
		
		@Override
		public final double[] newInstance() {
			return new double[1];
		}
		
		@Override
		public final Class<double[]> getInstanceClass() {
			return double[].class;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8882463620764023721L;
		
	};
	
}
