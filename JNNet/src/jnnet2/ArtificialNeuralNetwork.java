package jnnet2;

import static java.lang.System.arraycopy;
import static jnnet2.JNNetTools.add;
import static jnnet2.JNNetTools.dSigmoid;
import static jnnet2.JNNetTools.getDeclaredField;
import static jnnet2.JNNetTools.sigmoid;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author codistmonk (creation 2013-12-20)
 */
public final class ArtificialNeuralNetwork implements Serializable {
	
	private final int inputCount;
	
	private double[] weights;
	
	private int[] inputIndices;
	
	private int[] neurons;
	
	private int[] outputNeurons;
	
	private double[] values;
	
	private double[] dValues;
	
	private double[] deltas;
	
	public ArtificialNeuralNetwork(final int inputCount) {
		this.inputCount = inputCount;
		this.weights = new double[0];
		this.inputIndices = new int[0];
		this.neurons = new int[0];
		this.outputNeurons = new int[0];
		this.values = new double[1 + inputCount];
		this.dValues = new double[1 + inputCount];
		this.deltas = new double[1 + inputCount];
		this.values[0] = 1.0;
	}
	
	public final int getInputCount() {
		return this.inputCount;
	}
	
	public final ArtificialNeuralNetwork addOutputNeuron(final int sourceOffset, final double... weights) {
		add(this, OUTPUT_NEURONS, this.weights.length);
		
		return this.addNeuron(sourceOffset, weights);
	}
	
	public final ArtificialNeuralNetwork addNeuron(final int sourceOffset, final double... weights) {
		add(this, NEURONS, this.weights.length);
		add(this, VALUES, 0.0);
		add(this, D_VALUES, 0.0);
		add(this, DELTAS, 0.0);
		add(this, INPUT_INDICES, 0);
		add(this, WEIGHTS, weights[0]);
		
		final int n = weights.length;
		
		for (int i = 1; i < n; ++i) {
			add(this, INPUT_INDICES, sourceOffset + i - 1);
			add(this, WEIGHTS, weights[i]);
		}
		
		return this;
	}
	
	public final double[] getWeights() {
		return this.weights;
	}
	
	public final int[] getInputIndices() {
		return this.inputIndices;
	}
	
	public final int[] getNeurons() {
		return this.neurons;
	}
	
	public final int[] getOutputNeurons() {
		return this.outputNeurons;
	}
	
	public final double[] getValues() {
		return this.values;
	}
	
	public final ArtificialNeuralNetwork evaluate(final double... inputs) {
		arraycopy(inputs, 0, this.getValues(), 1, this.getInputCount());
		
		final int neuronCount = this.getNeurons().length;
		
		for (int neuronIndex = 0; neuronIndex < neuronCount; ++neuronIndex) {
			final int neuron = this.getNeurons()[neuronIndex];
			final int nextNeuron = this.getNextNeuron(neuronIndex);
			final int neuronValueIndex = 1 + this.getInputCount() + neuronIndex;
			this.getValues()[neuronValueIndex] = 0.0;
			
			for (int weightIndex = neuron; weightIndex < nextNeuron; ++weightIndex) {
				this.getValues()[neuronValueIndex] += this.getWeights()[weightIndex] * this.getValues()[this.getInputIndices()[weightIndex]];
			}
			
			this.deltas[neuronValueIndex] = 0.0;
			this.dValues[neuronValueIndex] = dSigmoid(this.getValues()[neuronValueIndex]);
			this.getValues()[neuronValueIndex] = sigmoid(this.getValues()[neuronValueIndex]);
		}
		
		return this;
	}
	
	private final int getNextNeuron(final int neuronIndex) {
		return neuronIndex + 1 < this.getNeurons().length ? this.getNeurons()[neuronIndex + 1] : this.getWeights().length;
	}
	
	public final double getOutputValue(final int outputNeuronIndex) {
		final int outputCount = this.getOutputNeurons().length;
		
		return this.getValues()[this.getValues().length - outputCount + outputNeuronIndex];
	}
	
	public final void backpropagate(final double learningRate, final double... errors) {
		final int outputCount = this.getOutputNeurons().length;
		
		for (int i = 0; i < outputCount; ++i) {
			final int neuronIndex = this.getNeurons().length - outputCount + i;
			final int neuronValueIndex = 1 + this.getInputCount() + neuronIndex;
			final double delta = this.deltas[neuronValueIndex] = errors[i] * this.dValues[neuronValueIndex];
			final int neuron = this.getNeurons()[neuronIndex];
			final int nextNeuron = this.getNextNeuron(neuronIndex);
			
			for (int weightIndex = neuron; weightIndex < nextNeuron; ++weightIndex) {
				final int sourceIndex = this.getInputIndices()[weightIndex];
				final double weight = this.getWeights()[weightIndex] += learningRate * delta * this.getValues()[sourceIndex];
				this.deltas[sourceIndex] += delta * weight;
			}
		}
		
		final int lastNonoutputNeuronIndex = this.getNeurons().length - 1 - this.getOutputNeurons().length;
		
		for (int neuronIndex = lastNonoutputNeuronIndex; 0 <= neuronIndex; --neuronIndex) {
			final int neuronValueIndex = 1 + this.getInputCount() + neuronIndex;
			final double delta = this.deltas[neuronValueIndex] *= this.dValues[neuronValueIndex];
			final int neuron = this.getNeurons()[neuronIndex];
			final int nextNeuron = this.getNextNeuron(neuronIndex);
			
			for (int weightIndex = neuron; weightIndex < nextNeuron; ++weightIndex) {
				final int sourceIndex = this.getInputIndices()[weightIndex];
				final double weight = this.getWeights()[weightIndex] += learningRate * delta * this.getValues()[sourceIndex];
				this.deltas[sourceIndex] += delta * weight;
			}
		}
	}
	
	@Override
	public final String toString() {
		return "\nweights: " + Arrays.toString(this.getWeights()) +
				"\ninputIndices: " + Arrays.toString(this.getInputIndices()) +
				"\nneurons: " + Arrays.toString(this.getNeurons()) +
				"\noutputNeurons: " + Arrays.toString(this.getOutputNeurons()) +
				"\nvalues: " + Arrays.toString(this.getValues()) +
				"\ndValues: " + Arrays.toString(this.dValues) +
				"\ndeltas: " + Arrays.toString(this.deltas)
		;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -5991429170095271425L;
	
	private static final Field WEIGHTS = getDeclaredField(ArtificialNeuralNetwork.class, "weights");
	
	private static final Field INPUT_INDICES = getDeclaredField(ArtificialNeuralNetwork.class, "inputIndices");
	
	private static final Field NEURONS = getDeclaredField(ArtificialNeuralNetwork.class, "neurons");
	
	private static final Field OUTPUT_NEURONS = getDeclaredField(ArtificialNeuralNetwork.class, "outputNeurons");
	
	private static final Field VALUES = getDeclaredField(ArtificialNeuralNetwork.class, "values");
	
	private static final Field D_VALUES = getDeclaredField(ArtificialNeuralNetwork.class, "dValues");
	
	private static final Field DELTAS = getDeclaredField(ArtificialNeuralNetwork.class, "deltas");
	
	/**
	 * @author codistmonk (creation 2013-12-21)
	 */
	public static final class Training implements Serializable {
		
		private final Item[] items;
		
		public Training(final Item... items) {
			this.items = items;
		}
		
		public final Item[] getItems() {
			return this.items;
		}
		
		public final void train(final ArtificialNeuralNetwork ann, final double learningRate) {
			for (final Item item : this.getItems()) {
				final double[] errors = item.computeErrors(ann);
				ann.backpropagate(learningRate * norm2(errors), errors);
			}
		}
		
		public static final double norm2(final double... v) {
			double result = 0.0;
			
			for (final double value : v) {
				result += value * value;
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3374064721124150195L;
		
		/**
		 * @author codistmonk (creation 2013-12-21)
		 */
		public static final class Item implements Serializable {
			
			private final double[] inputs;
			
			private final double[] outputs;
			
			public Item(final double[] inputs, final double[] outputs) {
				this.inputs = inputs;
				this.outputs = outputs;
			}
			
			public final double[] getInputs() {
				return this.inputs;
			}
			
			public final double[] getOutputs() {
				return this.outputs;
			}
			
			public final double[] computeErrors(final ArtificialNeuralNetwork ann) {
				ann.evaluate(this.getInputs());
				
				final int outputCount = ann.getOutputNeurons().length;
				final double[] result = new double[outputCount];
				
				for (int i = 0; i < outputCount; ++i) {
					result[i] = this.getOutputs()[i] - ann.getOutputValue(i);
				}
				
				return result;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -1465267783319711989L;
			
		}
		
	}
	
}
