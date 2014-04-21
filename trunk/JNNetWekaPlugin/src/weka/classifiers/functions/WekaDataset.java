package weka.classifiers.functions;

import static java.util.Arrays.copyOf;
import jnnet.Dataset;
import weka.core.Instances;

/**
 * @author codistmonk (creation 2014-04-20)
 */
public final class WekaDataset implements Dataset {
	
	private final Instances data;
	
	public WekaDataset(final Instances data) {
		this.data = data;
	}
	
	@Override
	public final int getItemCount() {
		return this.data.numInstances();
	}
	
	@Override
	public final int getItemSize() {
		return this.data.numAttributes();
	}
	
	@Override
	public final double getItemValue(final int itemId, final int valueId) {
		return this.data.instance(itemId).value(valueId);
	}
	
	@Override
	public final double[] getItem(final int itemId) {
		final double[] result = this.data.instance(itemId).toDoubleArray();
		final int sourceLabelIndex = this.data.classIndex();
		
		return convert(sourceLabelIndex, result);
	}
	
	@Override
	public final double[] getItemWeights(final int itemId) {
		return copyOf(this.getItem(itemId), this.getItemSize() - 1);
	}
	
	@Override
	public final double getItemLabel(final int itemId) {
		return this.data.instance(itemId).value(this.data.classIndex());
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2985421001512165702L;
	
	public static final double[] convert(final int sourceLabelIndex, final double[] result) {
		final int labelIndex = result.length - 1;
		
		if (sourceLabelIndex != labelIndex) {
			final double label = result[sourceLabelIndex];
			
			System.arraycopy(result, sourceLabelIndex + 1, result, sourceLabelIndex, labelIndex - sourceLabelIndex);
			
			result[labelIndex] = label;
		}
		
		for (int i = 0; i <= labelIndex; ++i) {
			final double value = result[i];
			
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				result[i] = 0.0;
			}
		}
		
		return result;
	}
	
}
