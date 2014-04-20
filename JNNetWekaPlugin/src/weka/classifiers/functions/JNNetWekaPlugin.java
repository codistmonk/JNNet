package weka.classifiers.functions;

import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static weka.classifiers.functions.WekaDataset.convert;

import java.util.Arrays;

import net.sourceforge.aprog.tools.Tools;
import jnnet4.BinaryClassifier;
import jnnet4.SimpleConfusionMatrix;
import jnnet4.SimplifiedNeuralBinaryClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.functions.WekaDataset;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.SupervisedFilter;

/**
 * @author codistmonk (creation 2014-04-20)
 */
public final class JNNetWekaPlugin extends Classifier implements SupervisedFilter {
	
	private BinaryClassifier classifier;
	
	private int sourceLabelIndex;
	
	@Override
	public final void buildClassifier(final Instances data) throws Exception {
		this.classifier = new SimplifiedNeuralBinaryClassifier(new WekaDataset(data));
		this.sourceLabelIndex = data.classIndex();
	}
	
	@Override
	public final double classifyInstance(final Instance instance) throws Exception {
		final double result = this.classifier.accept(
				copyOf(convert(this.sourceLabelIndex, instance.toDoubleArray()), instance.numAttributes() - 1)) ? 1.0 : 0.0;
		
		return result;
	}
	
	final SimpleConfusionMatrix evaluate(final Instances data) {
		return this.classifier.evaluate(new WekaDataset(data), null);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -1418995195967354703L;
	
}
