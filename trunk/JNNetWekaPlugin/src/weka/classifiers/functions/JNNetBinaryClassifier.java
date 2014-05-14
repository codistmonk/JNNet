package weka.classifiers.functions;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.copyOf;
import static weka.classifiers.functions.WekaDataset.convert;
import static weka.core.Utils.getOption;

import java.util.Enumeration;
import java.util.Vector;

import jnnet.BinaryClassifier;
import jnnet.SimpleConfusionMatrix;
import jnnet.SimplifiedNeuralBinaryClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.functions.WekaDataset;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Capabilities.Capability;
import weka.filters.SupervisedFilter;

/**
 * @author codistmonk (creation 2014-04-20)
 */
public final class JNNetBinaryClassifier extends Classifier implements SupervisedFilter, OptionHandler {
	
	private BinaryClassifier classifier;
	
	private double acceptableErrorRate = 0.08;
	
	private int sourceLabelIndex;
	
	@Override
	public final Capabilities getCapabilities() {
	    final Capabilities result = new Capabilities(this);
	    
	    result.disableAll();
	    result.enable(Capability.NUMERIC_ATTRIBUTES);
	    result.enable(Capability.BINARY_CLASS);
	    
	    return result;
	}
	
	@Override
	public final String[] getOptions() {
		final Vector<String> result = new Vector<String>();
		
		for (final String option : super.getOptions()) {
			result.add(option);
		}
		
		result.add("-E");
		result.add("" + this.acceptableErrorRate);
		
		return result.toArray(new String[result.size()]);
	}
	
	@Override
	public final Enumeration<Option> listOptions() {
		final Vector<Option> resultBuilder = new Vector<>();
		@SuppressWarnings("unchecked")
		final Enumeration<Option> enumeration = super.listOptions();
		
		while (enumeration.hasMoreElements()) {
			resultBuilder.addElement(enumeration.nextElement());
		}
		
		resultBuilder.addElement(new Option(
				"\tAcceptable training error rate.\n" + "\t(default: 0.08)",
				"E", 1, "-E <double>"));
		
		return resultBuilder.elements();
	}
	
	@Override
	public final void setOptions(final String[] options) throws Exception {
		super.setOptions(options);
		
		this.setAcceptableErrorRate(parseDouble(getOption("E", options)));
	}
	
	public final String acceptableErrorRateTipText() {
		return "Acceptable training error rate";
	}
	
	public final double getAcceptableErrorRate() {
		return this.acceptableErrorRate;
	}
	
	public final void setAcceptableErrorRate(final double acceptableErrorRate) {
		this.acceptableErrorRate = acceptableErrorRate;
	}
	
	@Override
	public final void buildClassifier(final Instances data) throws Exception {
		this.classifier = new SimplifiedNeuralBinaryClassifier(new WekaDataset(data)
		, 0.5, this.acceptableErrorRate, 200, true, true);
		this.sourceLabelIndex = data.classIndex();
	}
	
	@Override
	public final double classifyInstance(final Instance instance) throws Exception {
		final double result = this.classifier.accept(
				copyOf(convert(this.sourceLabelIndex, instance.toDoubleArray()), instance.numAttributes() - 1)) ? 1.0 : 0.0;
		
		return result;
	}
	
	@Override
	protected final Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	final SimpleConfusionMatrix evaluate(final Instances data) {
		return this.classifier.evaluate(new WekaDataset(data), null);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -1418995195967354703L;
	
}
