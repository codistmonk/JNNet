package jnnet4;

import static java.util.Arrays.copyOf;
import static jnnet.DLLTools.loadDLL;
import static jnnet4.FeedforwardNeuralNetworkTest.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_ID;
import static jnnet4.FeedforwardNeuralNetworkTest.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_LINEAR;
import static jnnet4.FeedforwardNeuralNetworkTest.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_SIGMOID;
import static jnnet4.FeedforwardNeuralNetworkTest.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_THRESHOLD;
import static jnnet4.JNNetTools.sigmoid;
import static jnnet4.JNNetTools.uint8;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static org.junit.Assert.*;

import com.amd.aparapi.Kernel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import net.sourceforge.aprog.swing.SwingTools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-02)
 */
public final class FeedforwardNeuralNetworkTest {
	
	@Test
	public final void test1() {
		final boolean showNetwork = false;
		final FeedforwardNeuralNetwork network = new FeedforwardNeuralNetwork();
		
		try {
			assertEquals(1, network.getNeuronCount());
			assertEquals(0, network.getWeightCount());
			assertEquals(1, network.getLayerCount());
			
			network.execute(1);
			
			assertEquals("[1.0]", Arrays.toString(network.getNeuronValues()));
			
			network.newNeuron();
			network.newNeuron();
			network.newLayer();
			network.newNeuron();
			
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
			
			network.getWeights()[network.getNeuronFirstWeightId(3) + 0] = -1.0;
			network.getWeights()[network.getNeuronFirstWeightId(3) + 1] = +1.0;
			network.newNeuronSource(0, -0.5);
			network.getNeuronTypes()[3] = NEURON_TYPE_SUM_THRESHOLD;
			
			network.newNeuron();
			network.newNeuronSource(1, +1.0);
			network.newNeuronSource(2, -1.0);
			network.newNeuronSource(0, -0.5);
			network.getNeuronTypes()[4] = NEURON_TYPE_SUM_THRESHOLD;
			
			network.newLayer();
			
			network.newNeuron();
			network.newNeuronSource(3, +1.0);
			network.newNeuronSource(4, +1.0);
			network.newNeuronSource(0, -1.0);
			network.getNeuronTypes()[5] = NEURON_TYPE_SUM_THRESHOLD;
			
			network.getNeuronValues()[1] = 0.0;
			network.getNeuronValues()[2] = 1.0;
			network.execute(1);
			assertEquals("[1.0, 0.0, 1.0, 1.0, 0.0, 1.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronValues()[1] = 1.0;
			network.getNeuronValues()[2] = 0.0;
			network.execute(1);
			assertEquals("[1.0, 1.0, 0.0, 0.0, 1.0, 1.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronValues()[1] = 0.0;
			network.getNeuronValues()[2] = 0.0;
			network.execute(1);
			assertEquals("[1.0, 0.0, 0.0, 0.0, 0.0, 0.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronValues()[1] = 1.0;
			network.getNeuronValues()[2] = 1.0;
			network.execute(1);
			assertEquals("[1.0, 1.0, 1.0, 0.0, 0.0, 0.0]", Arrays.toString(network.getNeuronValues()));
			
			if (showNetwork) {
				show(network, 256, 100.0);
			}
		} finally {
			network.dispose();
		}
	}
	
	@Test
	public final void test2() {
		final FeedforwardNeuralNetwork network = new FeedforwardNeuralNetwork();
		
		try {
			network.newNeuron();
			network.newNeuron();
			network.newLayer();
			network.newNeuron();
			network.newNeuronSource(1, 1.0);
			network.newNeuronSource(2, 2.0);
			
			network.getNeuronValues()[1] = 3.0;
			network.getNeuronValues()[2] = 4.0;
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_SUM_ID;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, 11.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_SUM_LINEAR;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, 1.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_SUM_SIGMOID;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, " + sigmoid(11.0) + "]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_SUM_THRESHOLD;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, 1.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_MAX_ID;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, 8.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_MAX_LINEAR;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, 1.0]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_MAX_SIGMOID;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, " + sigmoid(8.0) + "]", Arrays.toString(network.getNeuronValues()));
			
			network.getNeuronTypes()[3] = FeedforwardNeuralNetwork.NEURON_TYPE_MAX_THRESHOLD;
			network.execute(1);
			assertEquals("[1.0, 3.0, 4.0, 1.0]", Arrays.toString(network.getNeuronValues()));
		} finally {
			network.dispose();
		}
	}
	
	@Test
	public final void test3() {
		final boolean showNetwork = false;
		final double[] trainingData = {
				-4.0, 0.0,
				-2.0, 1.0,
				0.0, 3.0,
				1.5, -1.0,
				4.0, 0.0
		};
		
		for (final byte hiddenNeuronType : new byte[] { NEURON_TYPE_SUM_THRESHOLD, NEURON_TYPE_SUM_LINEAR }) {
			final FeedforwardNeuralNetwork network = newFunctionApproximation1D(
					hiddenNeuronType, 1,
					trainingData
					);
			
			if (showNetwork) {
				show(network, 256, 40.0);
			}
			
			try {
				final int outputId = network.getNeuronCount() - 1;
				
				network.getNeuronValues()[1] = trainingData[0] - 1.0;
				network.execute(1);
				assertEquals("0.0", "" + network.getNeuronValues()[outputId]);
				
				for (int i = 0; i < trainingData.length; i += 2) {
					network.getNeuronValues()[1] = trainingData[i];
					network.execute(1);
					assertEquals("" + trainingData[i + 1], "" + network.getNeuronValues()[outputId]);
				}
				
				network.getNeuronValues()[1] = trainingData[trainingData.length - 2] + 1.0;
				network.execute(1);
				assertEquals("0.0", "" + network.getNeuronValues()[outputId]);
			} finally {
				network.dispose();
			}
		}
	}
	
	@Test
	public final void test4() {
		final boolean showNetwork = false;
		final double[] trainingData = {
				-4.0, 0.0, 0.0,
				-2.0, 1.0, -1.0,
				0.0, 3.0, 2.0,
				1.5, -1.0, 3.0,
				4.0, 0.0, 0.0
		};
		
		for (final byte hiddenNeuronType : new byte[] { NEURON_TYPE_SUM_THRESHOLD, NEURON_TYPE_SUM_LINEAR }) {
			final FeedforwardNeuralNetwork network = newFunctionApproximation1D(
					hiddenNeuronType, 2,
					trainingData
					);
			
			if (showNetwork) {
				show(network, 256, 40.0);
			}
			
			assertEquals(3, network.getLayerCount());
			assertEquals(2, network.getLayerNeuronCount(0));
			assertEquals(2, network.getLayerNeuronCount(network.getLayerCount() - 1));
			
			try {
				final int outputId = network.getNeuronCount() - 2;
				
				network.getNeuronValues()[1] = trainingData[0] - 1.0;
				network.execute(1);
				assertEquals("0.0", "" + network.getNeuronValues()[outputId + 0]);
				assertEquals("0.0", "" + network.getNeuronValues()[outputId + 1]);
				
				for (int i = 0; i < trainingData.length; i += 3) {
					network.getNeuronValues()[1] = trainingData[i];
					network.execute(1);
					assertEquals("" + trainingData[i + 1], "" + network.getNeuronValues()[outputId + 0]);
					assertEquals("" + trainingData[i + 2], "" + network.getNeuronValues()[outputId + 1]);
				}
				
				network.getNeuronValues()[1] = trainingData[trainingData.length - 3] + 1.0;
				network.execute(1);
				assertEquals("0.0", "" + network.getNeuronValues()[outputId + 0]);
				assertEquals("0.0", "" + network.getNeuronValues()[outputId + 1]);
			} finally {
				network.dispose();
			}
		}
	}
	
	static {
		loadDLL("aparapi");
	}
	
	public static final FeedforwardNeuralNetwork newFunctionApproximation1D(final byte hiddenNeuronType,
			final int outputDimension, final double... inputOutputs) {
		final FeedforwardNeuralNetwork result = new FeedforwardNeuralNetwork();
		
		result.newNeuron();
		
		result.newLayer();
		
		final int n = inputOutputs.length;
		final int step = 1 + outputDimension;
		
		for (int i = 0; i < n; i += step) {
			final double input = inputOutputs[i];
			final double previous = step <= i ? inputOutputs[i - step] : input;
			final double next = i + step < n ? inputOutputs[i + step] : input;
			
			result.newNeuron(hiddenNeuronType);
			
			if (hiddenNeuronType == NEURON_TYPE_SUM_THRESHOLD) {
				result.newNeuronSource(0, -input + (input - previous) / 2.0);
				result.newNeuronSource(1, 1.0);
			} else if (hiddenNeuronType == NEURON_TYPE_SUM_LINEAR) {
				// a * input + b = 0.5
				// a * previous + b = -0.5
				// det = input - previous
				// a = (0.5 - (-0.5)) / det = 1.0 / det
				// b = (input * -0.5 - previous * 0.5) / det = -(input + previous) / det / 2.0
				final double d = input - previous;
				
				if (d != 0.0) {
					result.newNeuronSource(0, -(input + previous) / d / 2.0);
					result.newNeuronSource(1, 1.0 / d);
				} else {
					result.newNeuronSource(0, 0.5);
					result.newNeuronSource(1, 0.0);
				}
			}
			
			result.newNeuron(hiddenNeuronType);
			
			if (hiddenNeuronType == NEURON_TYPE_SUM_THRESHOLD) {
				result.newNeuronSource(0, -input - (next - input) / 2.0);
				result.newNeuronSource(1, 1.0);
			} else if (hiddenNeuronType == NEURON_TYPE_SUM_LINEAR) {
				// a * next + b = 0.5
				// a * input + b = -0.5
				// det = next - input
				final double d = next - input;
				
				if (d != 0.0) {
					result.newNeuronSource(0, -(input + next) / d / 2.0);
					result.newNeuronSource(1, 1.0 / d);
				} else {
					result.newNeuronSource(0, -0.5);
					result.newNeuronSource(1, 0.0);
				}
			}
		}
		
		result.newLayer();
		
		for (int d = 1; d <= outputDimension; ++d) {
			result.newNeuron(NEURON_TYPE_SUM_ID);
			
			for (int i = 0; i < n; i += step) {
				final double output = inputOutputs[i + d];
				
				result.newNeuronSource(2 * (1 + i / step), output);
				result.newNeuronSource(2 * (1 + i / step) + 1, -output);
			}
		}
		
		return result;
	}
	
	public static final void show(final FeedforwardNeuralNetwork network, final int imageSize, final double scale) {
		final BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_3BYTE_BGR);
		
		draw(network, image, scale);
		
		SwingTools.show(image, getCallerClass().getName(), true);
	}
	
	public static final void draw(final FeedforwardNeuralNetwork network, final BufferedImage image, final double scale) {
		final int inputDimension = network.getLayerNeuronCount(0) - 1;
		final int outputDimension = network.getLayerNeuronCount(network.getLayerCount() - 1);
		
		if ((inputDimension != 1 && inputDimension != 2) || (outputDimension != 1 && outputDimension != 3)) {
			throw new IllegalArgumentException();
		}
		
		final int w = image.getWidth();
		final int h = image.getHeight();
		
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				network.getNeuronValues()[1] = (x - w / 2.0) / scale;
				
				if (inputDimension == 2) {
					network.getNeuronValues()[2] = (h / 2.0 - y) / scale;
				}
				
				network.update(0);
				
				if (inputDimension == 1 && outputDimension == 1) {
					final int yy = (int) (h / 2 - scale * network.getNeuronValues()[network.getNeuronCount() - 1]);
					image.setRGB(x, yy, Color.WHITE.getRGB());
				} else if (inputDimension == 2) {
					image.setRGB(x, y, getOutputAsRGB(network));
				}
			}
			
			if (inputDimension == 1) {
				break;
			}
		}
	}

	public static int getOutputAsRGB(final FeedforwardNeuralNetwork network) {
		final int outputDimension = network.getLayerNeuronCount(network.getLayerCount() - 1);
		int result = 0xFF000000;
		
		if (outputDimension == 1) {
			result |= uint8(network.getNeuronValues()[network.getNeuronCount() - 1]) * 0x00010101;
		} else if (outputDimension == 3) {
			final int red = uint8(network.getNeuronValues()[network.getNeuronCount() - 3]);
			final int green = uint8(network.getNeuronValues()[network.getNeuronCount() - 2]);
			final int blue = uint8(network.getNeuronValues()[network.getNeuronCount() - 1]);
			result |= (red << 16) | (green << 8) | (blue << 0);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-03-02)
	 */
	public static final class FeedforwardNeuralNetwork extends Kernel {
		
		private double[] neuronValues = { 1.0 };
		
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
			
			if (this.getNeuronValues().length != result) {
				throw new IllegalStateException();
			}
			
			final int weightCount = this.getWeightCount();
			
			this.neuronValues = copyOf(this.getNeuronValues(), result + 1);
			this.neuronTypes = copyOf(this.getNeuronTypes(), result + 1);
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
		
		public final int getNeuronFirstWeightId(final int neuronId) {
			return this.neuronFirstWeightIds[neuronId];
		}
		
		public final int getNeuronLastWeightId(final int neuronId) {
			return this.getNeuronFirstWeightId(neuronId + 1) - this.getNeuronFirstWeightId(neuronId);
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
		
	}
	
}
