package jnnet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author codistmonk (creation 2013-12-17)
 */
public final class Network implements Serializable {
	
	private final List<ModifiableValueSource> inputs;
	
	private final List<Neuron> neurons;
	
	private final List<Neuron> outputs;
	
	public Network() {
		this.inputs = new ArrayList<>();
		this.neurons = new ArrayList<>();
		this.outputs = new ArrayList<>();
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
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2125463032324232908L;
	
}
	