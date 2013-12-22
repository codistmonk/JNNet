package jnnet3;

import static java.lang.System.arraycopy;
import static jnnet3.JNNetTools.add;
import static jnnet3.JNNetTools.getDeclaredField;
import static jnnet3.JNNetTools.sigmoid;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

import net.sourceforge.aprog.tools.Tools;

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
	
	public ArtificialNeuralNetwork(final int inputCount) {
		this.inputCount = inputCount;
		this.weights = new double[0];
		this.inputIndices = new int[0];
		this.neurons = new int[0];
		this.outputNeurons = new int[0];
		this.values = new double[1 + inputCount];
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
	
	@Override
	public final String toString() {
		return "\nweights: " + Arrays.toString(this.getWeights()) +
				"\ninputIndices: " + Arrays.toString(this.getInputIndices()) +
				"\nneurons: " + Arrays.toString(this.getNeurons()) +
				"\noutputNeurons: " + Arrays.toString(this.getOutputNeurons()) +
				"\nvalues: " + Arrays.toString(this.getValues())
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
	
	/**
	 * @author codistmonk (creation 2013-12-21)
	 */
	public static final class Training implements Serializable {
		
		private final Item[] items;
		
		private final Random random;
		
		public Training(final Item... items) {
			this.items = items;
			this.random = new Random(items.length);
		}
		
		public final Item[] getItems() {
			return this.items;
		}
		
		public final void train(final ArtificialNeuralNetwork ann, final double learningRate) {
			final int algo = 0;
			
			if (algo == 0) {
				final int weightCount = ann.getWeights().length;
				double error = this.computeError(ann);
				
				for (int i = 0; i < weightCount; ++i) {
					final double dw = (random.nextDouble() - 0.5) * learningRate * error;
					final double weight = ann.getWeights()[i];
					
					ann.getWeights()[i] = weight + dw;
					
					final double newError = this.computeError(ann);
					
					if (error <= newError) {
						ann.getWeights()[i] = weight;
					} else {
						error = newError;
					}
				}
			} else if (algo == 1) {
				
			}
		}
		
		public final double computeError(final ArtificialNeuralNetwork ann) {
			double result = 0.0;
			
			for (final Item item : this.getItems()) {
				result += norm2(item.computeErrors(ann));
			}
			
			return result;
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
