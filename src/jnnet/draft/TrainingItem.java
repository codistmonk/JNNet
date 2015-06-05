package jnnet.draft;

import static multij.tools.MathTools.square;

import java.io.Serializable;

import jnnet.draft.Neuron.Input;

/**
 * @author codistmonk (creation 2013-12-17)
 */
public final class TrainingItem implements Serializable {
	
	private final double[] inputs;
	
	private final double[] outputs;
	
	public TrainingItem(final double[] inputs, final double[] outputs) {
		this.inputs = inputs;
		this.outputs = outputs;
	}
	
	public final double[] getInputs() {
		return this.inputs;
	}
	
	public final double[] getOutputs() {
		return this.outputs;
	}
	
	public final void train(final Network network, final double weightDelta) {
		this.setInputs(network);
		
		double error = this.computeError(network);
		
		for (final Neuron neuron : network.getNeurons()) {
			for (final Input input : neuron.getInputs()) {
				final double weight = input.getWeight();
				
				input.setWeight(weight + weightDelta * error);
				
				double newError = this.computeError(network);
				
				if (error < newError) {
					input.setWeight(weight - weightDelta * error);
					newError = this.computeError(network);
				}
				
				if (error <= newError) {
					input.setWeight(weight);
				} else {
					error = newError;
				}
			}
		}
	}
	
	public final void setInputs(final Network network) {
		final int n = this.getInputs().length;
		
		for (int i = 0; i < n; ++i) {
			network.getInputs().get(i).setValue(this.getInputs()[i]);
		}
	}
	
	public final double computeError(final Network network) {
		network.update();
		
		final int n = this.getOutputs().length;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result += square(network.getOutputs().get(i).getValue() - this.getOutputs()[i]);
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 7239242132428142945L;
	
}

