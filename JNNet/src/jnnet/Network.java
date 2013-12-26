package jnnet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jnnet.Neuron.Input;

/**
 * @author codistmonk (creation 2013-12-17)
 */
public final class Network implements Serializable {
	
	private final List<ModifiableValueSource> inputs;
	
	private final List<Neuron> neurons;
	
	private final List<Neuron> outputs;
	
	public Network() {
		this.inputs = new ArrayList<ModifiableValueSource>();
		this.neurons = new ArrayList<Neuron>();
		this.outputs = new ArrayList<Neuron>();
	}
	
	public final List<ModifiableValueSource> getInputs() {
		return this.inputs;
	}
	
	public final List<Neuron> getNeurons() {
		return this.neurons;
	}
	
	public final List<Neuron> getOutputs() {
		return this.outputs;
	}
	
	public final void update() {
		for (final Neuron neuron : this.getNeurons()) {
			neuron.update();
		}
	}
	
	public final Network addInput(final ModifiableValueSource input) {
		this.getInputs().add(input);
		
		return this;
	}
	
	public final Network addNeuron(final Neuron neuron) {
		this.getNeurons().add(neuron);
		
		return this;
	}
	
	public final Network addOutput(final Neuron neuron) {
		this.getOutputs().add(neuron);
		
		return this.addNeuron(neuron);
	}
	
	@Override
	public final String toString() {
		final StringBuilder resultBuilder = new StringBuilder();
		
		resultBuilder.append('\n');
		resultBuilder.append("weights: ");
		
		for (final Neuron neuron : this.getNeurons()) {
			for (final Input input : neuron.getInputs()) {
				resultBuilder.append(input.getWeight());
				resultBuilder.append(' ');
			}
		}
		
		resultBuilder.append('\n');
		resultBuilder.append("values: ");
		
		for (final Neuron neuron : this.getNeurons()) {
			resultBuilder.append(neuron.getValue());
			resultBuilder.append(' ');
		}
		
		return resultBuilder.toString();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2125463032324232908L;
	
}
	