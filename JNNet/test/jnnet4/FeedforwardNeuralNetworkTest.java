package jnnet4;

import static java.util.Arrays.copyOf;
import static jnnet4.FeedforwardNeuralNetworkTest.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_SIGMOID;
import static jnnet4.JNNetTools.sigmoid;
import static jnnet4.JNNetTools.uint8;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import net.sourceforge.aprog.swing.SwingTools;

import org.junit.Test;

import com.amd.aparapi.Kernel;

/**
 * @author codistmonk (creation 2014-03-02)
 */
public final class FeedforwardNeuralNetworkTest {
	
	@Test
	public final void test1() {
		final FeedforwardNeuralNetwork network = new FeedforwardNeuralNetwork();
		
		try {
			assertEquals(1, network.getNeuronCount());
			assertEquals(0, network.getWeightCount());
			assertEquals(1, network.getLayerCount());
			
			network.execute(1);
			
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
			network.getNeuronTypes()[3] = NEURON_TYPE_SUM_SIGMOID;
			
			network.execute(1);
			
			assertEquals("[1.0, 2.0, 3.0, 0.5]", Arrays.toString(network.getNeuronValues()));
			
			network.execute(1);
			
			assertEquals("[1.0, 2.0, 3.0, 0.5]", Arrays.toString(network.getNeuronValues()));
			
			network.newNeuronSource(1, 0.4);
			network.newNeuronSource(2, 0.6);
			
			network.execute(1);
			
			assertEquals("[1.0, 2.0, 3.0, " + sigmoid(0.4 * 2.0 + 0.6 * 3.0) + "]", Arrays.toString(network.getNeuronValues()));
			
			show(network, 512, 1.0);
		} finally {
			network.dispose();
		}
	}
	
	public static final void show(final FeedforwardNeuralNetwork network, final int imageSize, final double scale) {
		final BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_3BYTE_BGR);
		
		draw(network, image, scale);
		
		SwingTools.show(image, getCallerClass().getName(), true);
	}
	
	public static final void draw(final FeedforwardNeuralNetwork network, final BufferedImage image, final double scale) {
		final int inputDimension = network.getLayerNeuronCount(0) - 1;
		final int outputDimension = network.getLayerNeuronCount(network.getLayerCount() - 1);
		
		if (inputDimension != 2 || (outputDimension != 1 && outputDimension != 3)) {
			throw new IllegalArgumentException();
		}
		
		final int w = image.getWidth();
		final int h = image.getHeight();
		
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				final double inputX = scale * (x - w / 2.0);
				final double inputY = scale * (h / 2.0 - y);
				network.getNeuronValues()[1] = inputX;
				network.getNeuronValues()[2] = inputY;
				network.update(0);
				int rgb = 0xFF000000;
				
				if (outputDimension == 1) {
					rgb |= uint8(network.getNeuronValues()[network.getNeuronCount() - 1]) * 0x00010101;
				} else if (outputDimension == 3) {
					final int red = uint8(network.getNeuronValues()[network.getNeuronCount() - 3]);
					final int green = uint8(network.getNeuronValues()[network.getNeuronCount() - 2]);
					final int blue = uint8(network.getNeuronValues()[network.getNeuronCount() - 1]);
					rgb |= (red << 16) | (green << 8) | (blue << 0);
				}
				
				image.setRGB(x, y, rgb);
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-03-02)
	 */
	public final class FeedforwardNeuralNetwork extends Kernel {
		
		private double[] neuronValues = { 0.0 };
		
		private byte[] neuronTypes = { 0 };
		
		private int[] neuronFirstWeightIds = { 1, 1 };
		
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
				final int weightIdStart = this.neuronFirstWeightIds[neuronId];
				final int weightIdEnd = this.neuronFirstWeightIds[neuronId + 1];
				final int neurontType = this.getNeuronTypes()[neuronId];
				double aggregate = 0.0;
				
				if (neurontType == NEURON_TYPE_SUM_ID) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate += weight * sourceValue;
					}
				} else if (neurontType == NEURON_TYPE_SUM_CLAMP) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate += weight * sourceValue;
					}
					
					aggregate = max(-1.0, min(aggregate, 1.0));
				} else if (neurontType == NEURON_TYPE_SUM_SIGMOID) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate += weight * sourceValue;
					}
					
					aggregate = sigmoid(aggregate);
				} else if (neurontType == NEURON_TYPE_SUM_STEP) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate += weight * sourceValue;
					}
					
					aggregate = 0.5 <= aggregate ? 1.0 : 0.0;
				} else if (neurontType == NEURON_TYPE_MAX_ID) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate = max(aggregate, weight * sourceValue);
					}
				} else if (neurontType == NEURON_TYPE_MAX_CLAMP) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate = max(aggregate, weight * sourceValue);
					}
					
					aggregate = max(-1.0, min(aggregate, 1.0));
				} else if (neurontType == NEURON_TYPE_MAX_SIGMOID) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate = max(aggregate, weight * sourceValue);
					}
					
					aggregate = sigmoid(aggregate);
				} else if (neurontType == NEURON_TYPE_MAX_STEP) {
					for (int weightId = weightIdStart; weightId < weightIdEnd; ++weightId) {
						final double weight = this.getWeights()[weightId];
						final int sourceId = neuronOffset + this.getSourceIds()[weightId];
						final double sourceValue = this.getNeuronValues()[sourceId];
						aggregate = max(aggregate, weight * sourceValue);
					}
					
					aggregate = 0.5 <= aggregate ? 1.0 : 0.0;
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
			
			if (this.getNeuronValues().length != result) {
				throw new IllegalStateException();
			}
			
			final int weightCount = this.getWeightCount();
			
			this.neuronValues = copyOf(this.getNeuronValues(), result + 1);
			this.neuronTypes = copyOf(this.getNeuronTypes(), result + 1);
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
		
		public final int getLayerNeuronCount(final int layerId) {
			return this.layerFirstNeuronIds[layerId + 1] - this.layerFirstNeuronIds[layerId];
		}
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_SUM_ID = 0;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_SUM_CLAMP = 1;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_SUM_SIGMOID = 2;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_SUM_STEP = 3;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_MAX_ID = 4;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_MAX_CLAMP = 5;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_MAX_SIGMOID = 6;
		
		/**
		 * {@value}.
		 */
		public static final byte NEURON_TYPE_MAX_STEP = 7;
		
	}
	
}
