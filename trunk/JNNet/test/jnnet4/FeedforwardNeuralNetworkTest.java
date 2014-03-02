package jnnet4;

import static java.util.Arrays.copyOf;
import static jnnet4.JNNetTools.sigmoid;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import com.amd.aparapi.Kernel;

/**
 * @author codistmonk (creation 2014-03-02)
 */
public final class FeedforwardNeuralNetworkTest {

	@Test
	public final void test1() {
		final FeedforwardNeuralNetwork network = new FeedforwardNeuralNetwork();
		
		assertEquals(1, network.getNeuronCount());
		assertEquals(0, network.getWeightCount());
		assertEquals(1, network.getLayerCount());
		
		network.update(0);
		
		assertEquals("[0.0]", Arrays.toString(network.getNeuronValues()));
		
		network.getNeuronValues()[0] = 42.0;
		
		assertEquals("[42.0]", Arrays.toString(network.getNeuronValues()));
		
		network.newNeuron();
		network.newNeuron();
		network.newLayer();
		network.newNeuron();
		
		network.getNeuronValues()[0] = 1.0;
		network.getNeuronValues()[1] = 2.0;
		network.getNeuronValues()[2] = 3.0;
		
		network.update(0);
		
		assertEquals("[1.0, 2.0, 3.0, 0.5]", Arrays.toString(network.getNeuronValues()));
		
		network.update(0);
		
		assertEquals("[1.0, 2.0, 3.0, 0.5]", Arrays.toString(network.getNeuronValues()));
		
		network.newNeuronSource(1, 0.4);
		network.newNeuronSource(2, 0.6);
		
		network.update(0);
		
		assertEquals("[1.0, 2.0, 3.0, " + sigmoid(0.4 * 2.0 + 0.6 * 3.0) + "]", Arrays.toString(network.getNeuronValues()));
	}
	
	/**
	 * @author codistmonk (creation 2014-03-02)
	 */
	public final class FeedforwardNeuralNetwork extends Kernel {
		
		private double[] neuronValues = { 0.0 };
		
		private int[] neuronFirstWeightIds = { 1, 1 };
		
		private int neuronCount = 1;
		
		private double[] weights = {};
		
		private int weightCount = 0;
		
		private int[] sourceIds = {};
		
		private int[] layerFirstNeuronIds = { 0, 1 };
		
		private int layerCount = 1;
		
		@Override
		public final void run() {
			this.update(0);
		}
		
		public final double[] getNeuronValues() {
			return this.neuronValues;
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
				final int weightIdStart = this.neuronFirstWeightIds[neuronId];
				final int weightIdEnd = this.neuronFirstWeightIds[neuronId + 1];
				double aggregate = 0.0;
				
				for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
					aggregate += this.getWeights()[weightId] * this.getNeuronValues()[neuronOffset + this.getSourceIds()[weightId]];
				}
				
				this.getNeuronValues()[neuronOffset + neuronId] = sigmoid(aggregate);
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
			
			if (this.getNeuronValues().length != result) {
				throw new IllegalStateException();
			}
			
			final int weightCount = this.getWeightCount();
			
			this.neuronValues = copyOf(this.getNeuronValues(), result + 1);
			this.neuronFirstWeightIds = copyOf(this.neuronFirstWeightIds, result + 2);
			this.weights = copyOf(this.getWeights(), weightCount + 1);
			this.sourceIds = copyOf(this.getSourceIds(), weightCount + 1);
			
			this.neuronFirstWeightIds[result] = weightCount;
			this.neuronFirstWeightIds[result + 1] = weightCount + 1;
			++this.neuronCount;
			++this.weightCount;
			++this.layerFirstNeuronIds[this.getLayerCount()];
			
			return result;
		}
		
		public final int newNeuronSource(final int sourceNeuronId, final double weight) {
			final int result = this.getWeightCount();
			
			this.weights = copyOf(this.weights, result + 1);
			this.sourceIds = copyOf(this.sourceIds, result + 1);
			
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
		
	}
	
}
