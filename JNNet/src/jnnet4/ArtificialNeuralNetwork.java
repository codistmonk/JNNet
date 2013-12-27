package jnnet4;

import static jnnet4.JNNetTools.add;
import static jnnet4.JNNetTools.getDeclaredField;
import static jnnet4.JNNetTools.sigmoid;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author codistmonk (creation 2013-12-26)
 */
public final class ArtificialNeuralNetwork {
	
	private double[] weights;
	
	private int[] sources;
	
	private int[] layers;
	
	private double[] values;
	
	private int[] firstWeightIndices;
	
	public ArtificialNeuralNetwork(final int inputCount) {
		if (inputCount < 1) {
			throw new IllegalArgumentException();
		}
		
		this.weights = new double[0];
		this.sources = new int[0];
		this.layers = new int[] { 0 };
		this.values = new double[inputCount];
		this.firstWeightIndices = new int[inputCount];
		
		Arrays.fill(this.firstWeightIndices, -1);
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
		
		for (int i = 0; i < neuronWeightCount; ++i) {
			add(this, WEIGHTS, weights[i]);
			add(this, SOURCES, sourceOffset + i);
		}
		
		return this;
	}
	
	public final ArtificialNeuralNetwork setFirstValues(final double... values) {
		System.arraycopy(values, 0, this.values, 0, values.length);
		
		return this;
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
					this.values[neuronIndex] += this.getWeights()[weightIndex] * this.values[this.sources[weightIndex]];
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
	
	private static final Field SOURCES = getDeclaredField(ArtificialNeuralNetwork.class, "sources");
	
	private static final Field LAYERS = getDeclaredField(ArtificialNeuralNetwork.class, "layers");
	
	private static final Field VALUES = getDeclaredField(ArtificialNeuralNetwork.class, "values");
	
	private static final Field FIRST_WEIGHT_INDICES = getDeclaredField(ArtificialNeuralNetwork.class, "firstWeightIndices");
	
}
