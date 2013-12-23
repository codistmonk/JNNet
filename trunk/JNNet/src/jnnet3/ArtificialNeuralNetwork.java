package jnnet3;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.util.Arrays.sort;
import static jnnet3.JNNetTools.add;
import static jnnet3.JNNetTools.getDeclaredField;
import static jnnet3.JNNetTools.sigmoid;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

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
	
	public final int getNextNeuron(final int neuronIndex) {
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
		
		public final void initializeWeights(final ArtificialNeuralNetwork ann) {
			sort(this.getItems(), new Comparator<Item>() {
				
				@Override
				public final int compare(final Item item1, final Item item2) {
					return DoubleArrayComparator.INSTANCE.compare(item1.getOutputs(), item2.getOutputs());
				}
				
			});
			
			final int itemCount = this.getItems().length;
			final int neuronCount = ann.getNeurons().length;
			
			for (int neuronIndex = 0, item1Index = 0, item2Index = itemCount - 1; neuronIndex < neuronCount; ++neuronIndex) {
				final int neuron = ann.getNeurons()[neuronIndex];
				final int nextNeuron = ann.getNextNeuron(neuronIndex);
				
				// TODO better layer handling
				debugPrint(neuronIndex, 1 + ann.getInputCount(), neuron, nextNeuron);
				if (1 + ann.getInputCount() < nextNeuron - neuron || ann.getOutputNeurons()[0] <= neuron) {
					break;
				}
				
				Item item1 = this.getItems()[item1Index];
				Item item2 = this.getItems()[item2Index];
				
				if (Arrays.equals(item1.getOutputs(), item2.getOutputs())) {
					item1Index = 0;
					item2Index = itemCount - 1;
					item1 = this.getItems()[item1Index];
					item2 = this.getItems()[item2Index];
				}
				
				final double[] item1Location = item1.getInputs();
				final double[] item2Location = item2.getInputs();
				final double[] neuronLocation = combine(item1Location, 0.5, item2Location, 0.5);
				
				ann.getWeights()[neuron] = 0.0;
				
				for (int weightIndex = neuron + 1; weightIndex < nextNeuron; ++weightIndex) {
					ann.getWeights()[neuron] -= ann.getWeights()[weightIndex] * neuronLocation[weightIndex - 1 - neuron];
				}
				
				debugPrint(Arrays.toString(neuronLocation), Arrays.toString(Arrays.copyOfRange(ann.getWeights(), neuron, nextNeuron)));
				
				++item1Index;
				--item2Index;
				
				if (item2Index <= item1Index) {
					item1Index = 0;
					item2Index = itemCount - 1;
				}
			}
		}
		
		public static final double[] combine(final double[] values1, final double weight1, final double[] values2, final double weight2) {
			final int n = values1.length;
			final double[] result = new double[n];
			
			for (int i = 0; i < n; ++i) {
				result[i] = values1[i] * weight1 + values2[i] * weight2;
			}
			
			return result;
		}
		
		public final void train(final ArtificialNeuralNetwork ann, final double learningRate) {
			final int algo = 0;
			
			if (algo == 0) {
				final int weightCount = ann.getWeights().length;
				double error = this.computeError(ann);
				
				for (int i = 0; i < weightCount; ++i) {
					final double dw = (this.random.nextDouble() - 0.5) * learningRate * error;
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
	
	/**
	 * @author codistmonk (creation 2013-12-23)
	 */
	public static final class DoubleArrayComparator implements Comparator<double[]>, Serializable {
		
		@Override
		public final int compare(final double[] array1, final double[] array2) {
			final int n = min(array1.length, array2.length);
			int result = 0;
			
			for (int i = 0; i < n && result == 0; ++i) {
				result = Double.compare(array1[i], array2[i]);
			}
			
			return result;
		}
		
		public static final DoubleArrayComparator INSTANCE = new DoubleArrayComparator();
		
	}
	
}
