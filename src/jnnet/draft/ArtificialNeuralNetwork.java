package jnnet.draft;

import static java.lang.Math.sqrt;
import static jnnet.JNNetTools.add;
import static jnnet.JNNetTools.getDeclaredField;
import static jnnet.JNNetTools.sigmoid;
import static multij.tools.MathTools.square;

import java.lang.reflect.Field;
import java.util.Arrays;

import multij.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-12-26)
 */
public final class ArtificialNeuralNetwork {
	
	private double[] weights;
	
	private int[] sourceIndices;
	
	private int[] layers;
	
	private double[] values;
	
	private int[] firstWeightIndices;
	
	private final int biasSourceIndex;
	
	public ArtificialNeuralNetwork(final int inputCount, final BiasSourceIndex biasSourceIndex) {
		if (inputCount < 1) {
			throw new IllegalArgumentException();
		}
		
		this.weights = new double[0];
		this.sourceIndices = new int[0];
		this.layers = new int[] { 0 };
		this.values = new double[inputCount];
		this.firstWeightIndices = new int[inputCount];
		this.biasSourceIndex = biasSourceIndex.getIndex(this);
		
		Arrays.fill(this.firstWeightIndices, -1);
		
		if (0 <= this.getBiasSourceIndex()) {
			this.values[this.getBiasSourceIndex()] = 1.0;
		}
	}
	
	public final int getVariableInputCount() {
		return this.getInputCount() - (0 <= this.getBiasSourceIndex() ? 1 : 0);
	}
	
	public final int getInputCount() {
		return this.getLayerNeuronCount(0);
	}
	
	public final int getBiasSourceIndex() {
		return this.biasSourceIndex;
	}
	
	public final int getSourceIndex(final int weightIndex) {
		return this.sourceIndices[weightIndex];
	}
	
	public final int getWeightIndex(final int neuronIndex, final int localWeightIndex) {
		return this.getNeuronFirstWeightIndex(neuronIndex) + localWeightIndex;
	}
	
	public final double getWeight(final int neuronIndex, final int localWeightIndex) {
		return this.getWeights()[this.getWeightIndex(neuronIndex, localWeightIndex)];
	}
	
	public final ArtificialNeuralNetwork setWeight(final int neuronIndex, final int localWeightIndex, final double weight) {
		this.getWeights()[this.getWeightIndex(neuronIndex, localWeightIndex)] = weight;
		
		return this;
	}
	
	public final ArtificialNeuralNetwork setBias(final int neuronIndex, final double bias) {
		if (this.getBiasSourceIndex() < 0) {
			return this;
		}
		
		if (this.getBiasSourceIndex() == 0) {
			return this.setWeight(neuronIndex, 0, bias);
		}
		
		return this.setWeight(neuronIndex, this.getNeuronWeightCount(neuronIndex) - 1, bias);
	}
	
	public final double getBias(final int neuronIndex) {
		if (this.getBiasSourceIndex() < 0) {
			return 0.0;
		}
		
		if (this.getBiasSourceIndex() == 0) {
			return this.getWeight(neuronIndex, 0);
		}
		
		return this.getWeight(neuronIndex, this.getNeuronWeightCount(neuronIndex) - 1);
	}
	
	public final double[] getWeights() {
		return this.weights;
	}
	
	public final int getNeuronCount() {
		return this.values.length;
	}
	
	public final int getLayerCount() {
		return this.layers.length;
	}
	
	public final int getLayerFirstNeuronIndex(final int layerIndex) {
		return this.layers[layerIndex];
	}
	
	public final int getLayerEndNeuronIndex(final int layerIndex) {
		final int nextLayerIndex = layerIndex + 1;
		
		return nextLayerIndex < this.getLayerCount() ? this.layers[nextLayerIndex] : this.getNeuronCount();
	}
	
	public final int getLayerNeuronCount(final int layerIndex) {
		return this.getLayerEndNeuronIndex(layerIndex) - this.getLayerFirstNeuronIndex(layerIndex);
	}
	
	public final int getNeuronFirstWeightIndex(final int neuronIndex) {
		return this.firstWeightIndices[neuronIndex];
	}
	
	public final int getNeuronEndWeightIndex(final int neuronIndex) {
		final int nextNeuronIndex = neuronIndex + 1;
		
		return nextNeuronIndex < this.getNeuronCount() ?
				this.firstWeightIndices[nextNeuronIndex] : this.getWeights().length;
	}
	
	public final int getNeuronWeightCount(final int neuronIndex) {
		final int firstWeightIndex = this.getNeuronFirstWeightIndex(neuronIndex);
		
		return firstWeightIndex < 0 ? 0 : this.getNeuronEndWeightIndex(neuronIndex) - firstWeightIndex;
	}
	
	public final double getNeuronValue(final int neuronIndex) {
		return this.values[neuronIndex];
	}
	
	public final ArtificialNeuralNetwork addLayer() {
		add(this, LAYERS, this.getNeuronCount());
		
		return this;
	}
	
	public final ArtificialNeuralNetwork addNeuron(final double... weights) {
		final int sourceLayerIndex = this.getLayerCount() - 2;
		final int neuronWeightCount = weights.length;
		
		if (this.getLayerNeuronCount(sourceLayerIndex) != neuronWeightCount) {
			throw new IllegalStateException();
		}
		
		final int sourceOffset = this.getLayerFirstNeuronIndex(sourceLayerIndex);
		
		add(this, VALUES, 0.0);
		add(this, FIRST_WEIGHT_INDICES, this.getWeights().length);
		
		if (this.getBiasSourceIndex() < 0) {
			for (int i = 0; i < neuronWeightCount; ++i) {
				add(this, WEIGHTS, weights[i]);
				add(this, SOURCE_INDICES, sourceOffset + i);
			}
		} else if (this.getBiasSourceIndex() == 0) {
			add(this, WEIGHTS, weights[0]);
			add(this, SOURCE_INDICES, 0);
			
			for (int i = 1; i < neuronWeightCount; ++i) {
				add(this, WEIGHTS, weights[i]);
				add(this, SOURCE_INDICES, sourceOffset + i);
			}
		} else {
			for (int i = 0; i < neuronWeightCount - 1; ++i) {
				add(this, WEIGHTS, weights[i]);
				add(this, SOURCE_INDICES, sourceOffset + i);
			}
			
			add(this, WEIGHTS, weights[neuronWeightCount - 1]);
			add(this, SOURCE_INDICES, this.getBiasSourceIndex());
		}
		
		return this;
	}
	
	public final void decomposeNeuron(final int neuronIndex, final double[] sharpnessOffsetDirection) {
		final int firstWeightIndex = this.getNeuronFirstWeightIndex(neuronIndex);
		final int endWeightIndex = this.getNeuronEndWeightIndex(neuronIndex);
		final int firstNonbiasWeightIndex = firstWeightIndex + (this.getBiasSourceIndex() == 0 ? 1 : 0);
		final int endNonbiasWeightIndex = endWeightIndex - (0 < this.getBiasSourceIndex() ? 1 : 0);
		double sharpness = 0.0;
		
		for (int weightIndex = firstNonbiasWeightIndex; weightIndex < endNonbiasWeightIndex; ++weightIndex) {
			sharpness += square(this.getWeights()[weightIndex]);
		}
		
		sharpness = sqrt(sharpness);
		sharpnessOffsetDirection[0] = sharpness;
		sharpnessOffsetDirection[1] = this.getBias(neuronIndex) / sharpness;
		
		for (int weightIndex = firstNonbiasWeightIndex; weightIndex < endNonbiasWeightIndex; ++weightIndex) {
			sharpnessOffsetDirection[weightIndex - firstNonbiasWeightIndex + 2] = this.getWeights()[weightIndex] / sharpness;
		}
	}
	
	public final void recomposeNeuron(final int neuronIndex, final double[] sharpnessOffsetDirection) {
		final int firstWeightIndex = this.getNeuronFirstWeightIndex(neuronIndex);
		final int endWeightIndex = this.getNeuronEndWeightIndex(neuronIndex);
		final int firstNonbiasWeightIndex = firstWeightIndex + (this.getBiasSourceIndex() == 0 ? 1 : 0);
		final int endNonbiasWeightIndex = endWeightIndex - (0 < this.getBiasSourceIndex() ? 1 : 0);
		final double sharpness = sharpnessOffsetDirection[0];
		
		this.setBias(neuronIndex, sharpnessOffsetDirection[1] * sharpness);
		
		for (int weightIndex = firstNonbiasWeightIndex; weightIndex < endNonbiasWeightIndex; ++weightIndex) {
			this.getWeights()[weightIndex] = sharpnessOffsetDirection[weightIndex - firstNonbiasWeightIndex + 2] * sharpness;
		}
	}
	
	public final ArtificialNeuralNetwork setValues(final int offset, final double... values) {
		System.arraycopy(values, 0, this.values, offset, values.length);
		
		return this;
	}
	
	public final ArtificialNeuralNetwork setInputs(final double... variableInputs) {
		if (variableInputs.length != this.getVariableInputCount()) {
			throw new IllegalArgumentException();
		}
		
		return this.setValues(this.getBiasSourceIndex() == 0 ? 1 : 0, variableInputs);
	}
	
	public final ArtificialNeuralNetwork updateAllLayers() {
		return this.updateFirstLayers(this.getLayerCount());
	}
	
	public final ArtificialNeuralNetwork updateFirstLayers(final int endLayerIndex) {
		for (int layerIndex = 1; layerIndex < endLayerIndex; ++layerIndex) {
			final int firstNeuronIndex = this.getLayerFirstNeuronIndex(layerIndex);
			final int endNeuronIndex = this.getLayerEndNeuronIndex(layerIndex);
			
			for (int neuronIndex = firstNeuronIndex; neuronIndex < endNeuronIndex; ++neuronIndex) {
				final int firstWeightIndex = this.getNeuronFirstWeightIndex(neuronIndex);
				final int endWeightIndex = this.getNeuronEndWeightIndex(neuronIndex);
				
				this.values[neuronIndex] = 0.0;
				
				for (int weightIndex = firstWeightIndex; weightIndex < endWeightIndex; ++weightIndex) {
					this.values[neuronIndex] += this.getWeights()[weightIndex] * this.values[this.sourceIndices[weightIndex]];
				}
				
				this.values[neuronIndex] = sigmoid(this.values[neuronIndex]);
			}
		}
		
		return this;
	}
	
	public final double[] getOutputLayerValues() {
		return this.getLayerValues(this.getLayerCount() - 1);
	}
	
	public final double[] getLayerValues(final int layerIndex) {
		final int firstNeuronIndex = this.getLayerFirstNeuronIndex(layerIndex);
		final int endNeuronIndex = this.getLayerEndNeuronIndex(layerIndex);
		final double[] result = new double[endNeuronIndex - firstNeuronIndex];
		
		for (int neuronIndex = firstNeuronIndex; neuronIndex < endNeuronIndex; ++neuronIndex) {
			result[neuronIndex - firstNeuronIndex] = this.getNeuronValue(neuronIndex);
		}
		
		return result;
	}
	
	private static final Field WEIGHTS = getDeclaredField(ArtificialNeuralNetwork.class, "weights");
	
	private static final Field SOURCE_INDICES = getDeclaredField(ArtificialNeuralNetwork.class, "sourceIndices");
	
	private static final Field LAYERS = getDeclaredField(ArtificialNeuralNetwork.class, "layers");
	
	private static final Field VALUES = getDeclaredField(ArtificialNeuralNetwork.class, "values");
	
	private static final Field FIRST_WEIGHT_INDICES = getDeclaredField(ArtificialNeuralNetwork.class, "firstWeightIndices");
	
	/**
	 * @author codistmonk (creation 2013-12-29)
	 */
	public static enum BiasSourceIndex {
		
		NONE {
			
			@Override
			public final int getIndex(final ArtificialNeuralNetwork network) {
				return -1;
			}
			
		}, FIRST {
			
			@Override
			public final int getIndex(final ArtificialNeuralNetwork network) {
				return 0;
			}
			
		}, LAST {
			
			@Override
			public final int getIndex(final ArtificialNeuralNetwork network) {
				return network.getLayerNeuronCount(0) - 1;
			}
			
		};
		
		public abstract int getIndex(ArtificialNeuralNetwork network);
		
	}
	
}
