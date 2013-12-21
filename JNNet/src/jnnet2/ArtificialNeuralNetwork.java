package jnnet2;

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
	
	public ArtificialNeuralNetwork(final int inputCount) {
		this.inputCount = inputCount;
		this.weights = new double[0];
		this.inputIndices = new int[0];
		this.neurons = new int[0];
		this.outputNeurons = new int[0];
		this.values = new double[1 + inputCount];
		this.dValues = new double[1 + inputCount];
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
		System.arraycopy(inputs, 0, this.getValues(), 1, this.getInputCount());
		
		final int neuronCount = this.getNeurons().length;
		
		for (int neuronIndex = 0; neuronIndex < neuronCount; ++neuronIndex) {
			final int neuron = this.getNeurons()[neuronIndex];
			final int nextNeuron = neuronIndex + 1 < neuronCount ? this.getNeurons()[neuronIndex + 1] : this.getWeights().length;
			final int neuronValueIndex = 1 + this.getInputCount() + neuronIndex;
			this.getValues()[neuronValueIndex] = 0.0;
			
			for (int i = neuron; i < nextNeuron; ++i) {
				this.getValues()[neuronValueIndex] += this.getWeights()[i] * this.getValues()[this.getInputIndices()[i]];
			}
			
			this.dValues[neuronValueIndex] = dSigmoid(this.getValues()[neuronValueIndex]);
			this.getValues()[neuronValueIndex] = sigmoid(this.getValues()[neuronValueIndex]);
		}
		
		return this;
	}
	
	public final double getOutputValue(final int outputNeuronIndex) {
		return this.getValues()[1 + this.getInputCount() + this.getOutputNeurons()[outputNeuronIndex]];
	}
	
	@Override
	public final String toString() {
		return "\nweights: " + Arrays.toString(this.getWeights()) +
				"\ninputIndices: " + Arrays.toString(this.getInputIndices()) +
				"\nneurons: " + Arrays.toString(this.getNeurons()) +
				"\noutputNeurons: " + Arrays.toString(this.getOutputNeurons()) +
				"\nvalues: " + Arrays.toString(this.getValues()) +
				"\ndValues: " + Arrays.toString(this.dValues)
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
		
		public final void train(final ArtificialNeuralNetwork ann) {
			for (final Item item : this.getItems()) {
				final double[] errors = item.computeErrors(ann);
			}
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
