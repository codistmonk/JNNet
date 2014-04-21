package jnnet.draft;

import static java.util.Arrays.copyOf;
import static jnnet.draft.JNNetTools.sigmoid;

import com.amd.aparapi.Kernel;

import java.util.BitSet;

/**
 * @author codistmonk (creation 2014-03-02)
 */
public final class FeedforwardNeuralNetwork extends Kernel {
	
	private double[] neuronValues = { 1.0 };
	
	private byte[] neuronTypes = { 0 };
	
	private int[] neuronFirstWeightIds = { 0, 0 };
	
	private int neuronCount = 1;
	
	private double[] weights = { Double.NaN };
	
	private int weightCount = 0;
	
	private int[] sourceIds = { -1 };
	
	private int[] layerFirstNeuronIds = { 0, 1 };
	
	private int layerCount = 1;
	
	@Override
	public final void run() {
		this.update(this.getGlobalId() * this.getNeuronCount());
	}
	
	public final double[] getNeuronValues() {
		return this.neuronValues;
	}
	
	public final byte[] getNeuronTypes() {
		return this.neuronTypes;
	}
	
	public final double[] getWeights() {
		return this.weights;
	}
	
	public final int[] getSourceIds() {
		return this.sourceIds;
	}
	
	public final void update(final int neuronOffset) {
		final int noninputLayerOffset = this.layerFirstNeuronIds[1];
		final int neuronIdEnd = neuronOffset + this.getNeuronCount();
		
		for (int neuronId = neuronOffset + noninputLayerOffset; neuronId < neuronIdEnd; ++neuronId) {
			final int weightIdStart = this.getNeuronFirstWeightId(neuronId);
			final int weightIdEnd = this.getNeuronFirstWeightId(neuronId + 1);
			final int neurontType = this.getNeuronTypes()[neuronId];
			double aggregate = 0.0;
			
			if (neurontType == NEURON_TYPE_SUM_ID) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate += weight * sourceValue;
				}
			} else if (neurontType == NEURON_TYPE_SUM_LINEAR) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate += weight * sourceValue;
				}
				
				aggregate = max(0.0, min(aggregate + 0.5, 1.0));
			} else if (neurontType == NEURON_TYPE_SUM_SIGMOID) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate += weight * sourceValue;
				}
				
				aggregate = sigmoid(aggregate);
			} else if (neurontType == NEURON_TYPE_SUM_THRESHOLD) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate += weight * sourceValue;
				}
				
				aggregate = 0.0 <= aggregate ? 1.0 : 0.0;
			} else if (neurontType == NEURON_TYPE_MAX_ID) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate = max(aggregate, weight * sourceValue);
				}
			} else if (neurontType == NEURON_TYPE_MAX_LINEAR) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate = max(aggregate, weight * sourceValue);
				}
				
				aggregate = max(0.0, min(aggregate + 0.5, 1.0));
			} else if (neurontType == NEURON_TYPE_MAX_SIGMOID) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate = max(aggregate, weight * sourceValue);
				}
				
				aggregate = sigmoid(aggregate);
			} else if (neurontType == NEURON_TYPE_MAX_THRESHOLD) {
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					final double weight = this.getWeights()[weightId];
					final int sourceId = neuronOffset + this.getSourceIds()[weightId];
					final double sourceValue = this.getNeuronValues()[sourceId];
					aggregate = max(aggregate, weight * sourceValue);
				}
				
				aggregate = 0.0 <= aggregate ? 1.0 : 0.0;
			}
			
			this.getNeuronValues()[neuronOffset + neuronId] = aggregate;
		}
	}
	
	public final int getNeuronCount() {
		return this.neuronCount;
	}
	
	public final int getWeightCount() {
		return this.weightCount;
	}
	
	public final int getLayerCount() {
		return this.layerCount;
	}
	
	public final int newNeuron() {
		final int result = this.getNeuronCount();
		final int weightCount = this.getWeightCount();
		
		this.neuronValues = reserve(this.getNeuronValues(), result + 1);
		this.neuronTypes = reserve(this.getNeuronTypes(), result + 1);
		this.neuronFirstWeightIds = copyOf(this.neuronFirstWeightIds, result + 2);
		
		this.neuronFirstWeightIds[result] = weightCount;
		this.neuronFirstWeightIds[result + 1] = weightCount;
		++this.neuronCount;
		++this.layerFirstNeuronIds[this.getLayerCount()];
		
		return result;
	}
	
	public final int newNeuron(final byte type) {
		final int result = this.newNeuron();
		
		this.getNeuronTypes()[result] = type;
		
		return result;
	}
	
	public final int newNeuronSource(final int sourceNeuronId, final double weight) {
		final int result = this.getWeightCount();
		
		this.weights = reserve(this.weights, result + 1);
		this.sourceIds = reserve(this.sourceIds, result + 1);
		
		this.getWeights()[result] = weight;
		this.getSourceIds()[result] = sourceNeuronId;
		++this.weightCount;
		++this.neuronFirstWeightIds[this.getNeuronCount()];
		
		return result;
	}
	
	public final int newLayer() {
		final int result = this.getLayerCount();
		
		this.layerFirstNeuronIds = copyOf(this.layerFirstNeuronIds, result + 2);
		
		this.layerFirstNeuronIds[result + 1] = this.getNeuronCount();
		++this.layerCount;
		
		return result;
	}
	
	public final int getLayerNeuronCount(final int layerId) {
		return this.layerFirstNeuronIds[layerId + 1] - this.layerFirstNeuronIds[layerId];
	}
	
	public final int getNeuronFirstWeightId(final int neuronId) {
		return this.neuronFirstWeightIds[neuronId];
	}
	
	public final int getNeuronWeightCount(final int neuronId) {
		return this.getNeuronFirstWeightId(neuronId + 1) - this.getNeuronFirstWeightId(neuronId);
	}
	
	public final void remove(final BitSet markedNeurons) {
		final int oldNeuronCount = this.getNeuronCount();
		final int newNeuronCount = oldNeuronCount - markedNeurons.cardinality();
		final int[] newNeuronIds = new int[oldNeuronCount];
		
		for (int oldNeuronId = 0, newNeuronId = 0; oldNeuronId < oldNeuronCount; ++oldNeuronId) {
			newNeuronIds[oldNeuronId] = newNeuronId;
			
			if (!markedNeurons.get(oldNeuronId)) {
				++newNeuronId;
			}
		}
		
		final double[] newNeuronValues = new double[newNeuronCount];
		final byte[] newNeuronTypes = new byte[newNeuronCount];
		final int oldWeightCount = this.getWeightCount();
		final BitSet markedWeights = new BitSet(oldWeightCount);
		
		for (int oldNeuronId = 0; oldNeuronId < oldNeuronCount; ++oldNeuronId) {
			final int newNeuronId = newNeuronIds[oldNeuronId];
			
			if (!markedNeurons.get(oldNeuronId)) {
				newNeuronValues[newNeuronId] = this.getNeuronValues()[oldNeuronId];
				newNeuronTypes[newNeuronId] = this.getNeuronTypes()[oldNeuronId];
			} else {
				final int oldNeuronFirstWeightId = this.getNeuronFirstWeightId(oldNeuronId);
				final int oldNeuronNextWeightId = this.getNeuronFirstWeightId(oldNeuronId + 1);
				
				markedWeights.set(oldNeuronFirstWeightId, oldNeuronNextWeightId);
			}
		}
		
		int newWeightCount = 0;
		
		for (int oldWeightId = 0; oldWeightId < oldWeightCount; ++oldWeightId) {
			if (!markedWeights.get(oldWeightId)) {
				if (!markedNeurons.get(this.getSourceIds()[oldWeightId])) {
					++newWeightCount;
				} else {
					markedWeights.set(oldWeightId);
				}
			}
		}
		
		final double[] newWeights = new double[newWeightCount];
		final int[] newSourceIds = new int[newWeightCount];
		final int[] newWeightIds = new int[oldWeightCount];
		
		for (int oldWeightId = 0, newWeightId = 0; oldWeightId < oldWeightCount; ++oldWeightId) {
			if (!markedWeights.get(oldWeightId)) {
				final int oldSourceId = this.getSourceIds()[oldWeightId];
				newSourceIds[newWeightId] = newNeuronIds[oldSourceId];
				newWeights[newWeightId] = this.getWeights()[oldWeightId];
				newWeightIds[oldWeightId] = newWeightId;
				++newWeightId;
			}
		}
		
		final int[] newNeuronFirstWeightIds = new int[newNeuronCount + 1];
		newNeuronFirstWeightIds[newNeuronCount] = newWeightCount;
		
		for (int oldNeuronId = oldNeuronCount - 1; 0 <= oldNeuronId; --oldNeuronId) {
			if (!markedNeurons.get(oldNeuronId)) {
				final int oldNeuronFirstWeightId = this.getNeuronFirstWeightId(oldNeuronId);
				final int oldNeuronNextWeightId = this.getNeuronFirstWeightId(oldNeuronId + 1);
				final int newNeuronId = newNeuronIds[oldNeuronId];
				newNeuronFirstWeightIds[newNeuronId] = newNeuronFirstWeightIds[newNeuronId + 1];
				
				for (int oldWeightId = oldNeuronFirstWeightId; oldWeightId < oldNeuronNextWeightId; ++oldWeightId) {
					if (!markedNeurons.get(this.getSourceIds()[oldWeightId])) {
						newNeuronFirstWeightIds[newNeuronId] = newWeightIds[oldWeightId];
						break;
					}
				}
			}
		}
		
		final int oldLayerCount = this.getLayerCount();
		final int newLayerCount = oldLayerCount;
		final int[] newLayerFirstNeuronIds = new int[newLayerCount + 1];
		newLayerFirstNeuronIds[newLayerCount] = newNeuronCount;
		
		for (int oldLayerId = oldLayerCount - 1; 0 <= oldLayerId; --oldLayerId) {
			final int newLayerId = oldLayerId;
			newLayerFirstNeuronIds[newLayerId] = newLayerFirstNeuronIds[newLayerId + 1];
			final int oldLayerFirstNeuronId = this.layerFirstNeuronIds[oldLayerId];
			final int oldLayerNextNeuronId = this.layerFirstNeuronIds[oldLayerId + 1];
			
			for (int oldNeuronId = oldLayerFirstNeuronId; oldNeuronId < oldLayerNextNeuronId; ++oldNeuronId) {
				if (!markedNeurons.get(oldNeuronId)) {
					newLayerFirstNeuronIds[newLayerId] = newNeuronIds[oldNeuronId];
					break;
				}
			}
		}
		
		this.neuronCount = newNeuronCount;
		this.neuronValues = newNeuronValues;
		this.neuronTypes = newNeuronTypes;
		this.neuronFirstWeightIds = newNeuronFirstWeightIds;
		this.weightCount = newWeightCount;
		this.weights = newWeights;
		this.sourceIds = newSourceIds;
		this.layerCount = newLayerCount;
		this.layerFirstNeuronIds = newLayerFirstNeuronIds;
	}
	
	public final FeedforwardNeuralNetwork pack(final int itemCount) {
		final int neuronCount = this.getNeuronCount();
		final int valueCount = max(1, neuronCount * itemCount);
		final int weightCount = this.getWeightCount();
		
		this.neuronTypes = pack(reserve(this.getNeuronTypes(), 1), neuronCount);
		this.neuronValues = pack(reserve(this.getNeuronValues(), valueCount), valueCount);
		this.neuronFirstWeightIds = pack(reserve(this.neuronFirstWeightIds, 1), neuronCount + 1);
		this.sourceIds = pack(reserve(this.getSourceIds(), 1), weightCount);
		this.weights = pack(reserve(this.getWeights(), 1), weightCount);
		
		return this;
	}
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_SUM_ID = 0;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_SUM_LINEAR = 1;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_SUM_SIGMOID = 2;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_SUM_THRESHOLD = 3;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_MAX_ID = 4;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_MAX_LINEAR = 5;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_MAX_SIGMOID = 6;
	
	/**
	 * {@value}.
	 */
	public static final byte NEURON_TYPE_MAX_THRESHOLD = 7;
	
	public static final byte[] pack(final byte[] array, final int maximumLength) {
		if (maximumLength < array.length) {
			return copyOf(array, maximumLength);
		}
		
		return array;
	}
	
	public static final int[] pack(final int[] array, final int maximumLength) {
		if (maximumLength < array.length) {
			return copyOf(array, maximumLength);
		}
		
		return array;
	}
	
	public static final double[] pack(final double[] array, final int maximumLength) {
		if (maximumLength < array.length) {
			return copyOf(array, maximumLength);
		}
		
		return array;
	}
	
	public static final byte[] reserve(final byte[] array, final int minimumLength) {
		if (array.length < minimumLength) {
			return copyOf(array, Math.max(array.length * 2, minimumLength));
		}
		
		return array;
	}
	
	public static final int[] reserve(final int[] array, final int minimumLength) {
		if (array.length < minimumLength) {
			return copyOf(array, Math.max(array.length * 2, minimumLength));
		}
		
		return array;
	}
	
	public static final double[] reserve(final double[] array, final int minimumLength) {
		if (array.length < minimumLength) {
			return copyOf(array, Math.max(array.length * 2, minimumLength));
		}
		
		return array;
	}
	
}
