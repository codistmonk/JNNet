package jnnet4;

import static java.util.Arrays.copyOfRange;
import static java.util.Collections.swap;
import static jnnet.DLLTools.loadDLL;
import static jnnet4.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_ID;
import static jnnet4.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_LINEAR;
import static jnnet4.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_SIGMOID;
import static jnnet4.FeedforwardNeuralNetwork.NEURON_TYPE_SUM_THRESHOLD;
import static jnnet4.VectorStatistics.add;
import static jnnet4.VectorStatistics.dot;
import static jnnet4.VectorStatistics.scaled;
import static jnnet4.VectorStatistics.subtract;
import static jnnet4.JNNetTools.sigmoid;
import static jnnet4.JNNetTools.uint8;
import static net.sourceforge.aprog.tools.Factory.DefaultFactory.HASH_SET_FACTORY;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getCallerClass;
import static net.sourceforge.aprog.tools.Tools.instances;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;

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
			
			assertEquals("[1.0, 2.0, 3.0, " + sigmoid(0.4 * 2.0 + 0.6 * 3.0) + "]",
					Arrays.toString(network.getNeuronValues()));
			
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
				show(network, 256, 100.0, null);
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
				show(network, 256, 40.0, null);
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
	
	@Test
	public final void test5() {
		final boolean showNetwork = false;
		final TrainingData trainingData = new TrainingData("jnnet/2spirals.txt");
		final int step = trainingData.getStep();
		final int inputDimension = step - 1;
		final double[] data = trainingData.getData();
		final FeedforwardNeuralNetwork network = newClassifier(trainingData);
		final int outputId = network.getNeuronCount() - 1;
		final SimpleConfusionMatrix confusionMatrix = new SimpleConfusionMatrix();
		
		for (int i = 0; i < data.length; i += step) {
			for (int j = 0; j < inputDimension; ++j) {
				network.getNeuronValues()[1 + j] = data[i + j];
			}
			
			network.update(0);
			
			final double expected = data[i + step - 1];
			final double actual = network.getNeuronValues()[outputId];
			
			if (expected != actual) {
				debugPrint(Arrays.toString(network.getSourceIds()));
				debugPrint(Arrays.toString(network.getWeights()));
				debugPrint(Arrays.toString(network.getNeuronValues()));
				
				if (expected == 1.0) {
					confusionMatrix.getFalseNegativeCount().incrementAndGet();
				} else {
					confusionMatrix.getFalsePositiveCount().incrementAndGet();
				}
			} else {
				if (expected == 1.0) {
					confusionMatrix.getTruePositiveCount().incrementAndGet();
				} else {
					confusionMatrix.getTrueNegativeCount().incrementAndGet();
				}
			}
			
			assertEquals("" + expected, "" + actual);
		}
		
		debugPrint(confusionMatrix);
		
		if (showNetwork) {
			show(network, 512, 20.0, data);
		}
		
		assertEquals(0L, confusionMatrix.getTotalErrorCount());
	}
	
	public static final FeedforwardNeuralNetwork newClassifier(final TrainingData trainingData) {
		final boolean debug = true;
		final FeedforwardNeuralNetwork result = new FeedforwardNeuralNetwork();
		final int step = trainingData.getStep();
		final int inputDimension = step - 1;
		final int n = trainingData.getItemCount();
		final double[] data = trainingData.getData();
		
		// Finish input layer
		{
			for (int i = 0; i < inputDimension; ++i) {
				result.newNeuron();
			}
		}
		
		// Build layer 1 (space partitioning)
		{
			result.newLayer();
			
			final List<List<Integer>> todo = new ArrayList<List<Integer>>();
			final Factory<VectorStatistics> vectorStatisticsFactory = DefaultFactory.forClass(VectorStatistics.class, inputDimension);
			
			todo.add(range(0, n - 1));
			
			while (!todo.isEmpty()) {
				final List<Integer> indices = todo.remove(0);
				final VectorStatistics[] statistics = instances(2, vectorStatisticsFactory);
				
				for (final int i : indices) {
					final int sampleOffset = i * step;
					final int labelOffset = sampleOffset + step - 1;
					statistics[(int) data[labelOffset]].addValues(copyOfRange(data, sampleOffset, labelOffset));
				}
				
				if (statistics[0].getCount() == 0 || statistics[1].getCount() == 0) {
					continue;
				}
				
				final double[] cluster0 = statistics[0].getMeans();
				final double[] cluster1 = statistics[1].getMeans();
				final double[] neuronWeights = subtract(cluster1, cluster0);
				
				if (Arrays.equals(cluster0, cluster1)) {
					for (int i = 0; i < inputDimension; ++i) {
						neuronWeights[i] = RANDOM.nextDouble();
					}
				}
				
				final double[] neuronLocation = scaled(add(cluster1, cluster0), 0.5);
				final double neuronBias = -dot(neuronWeights, neuronLocation);
				
				result.newNeuron(NEURON_TYPE_SUM_THRESHOLD);
				
				result.newNeuronSource(0, neuronBias);
				
				for (int i = 0; i < inputDimension; ++i) {
					result.newNeuronSource(i + 1, neuronWeights[i]);
				}
				
				{
					final int m = indices.size();
					int j = 0;
					
					for (int i = 0; i < m; ++i) {
						final int sampleOffset = indices.get(i) * step;
						final double d = dot(neuronWeights, copyOfRange(data, sampleOffset, sampleOffset + inputDimension)) + neuronBias;
						
						if (0 <= d) {
							swap(indices, i, j++);
						}
					}
					
					todo.add(indices.subList(0, j));
					todo.add(indices.subList(j, m));
				}
			}
		}
		
		final int layer1NeuronCount = result.getLayerNeuronCount(1);
		
		if (debug) {
			debugPrint("layer1NeuronCount:", layer1NeuronCount);
		}
		
		// Build layer 2 (convex regions definition)
		{
			@SuppressWarnings("unchecked")
			final Set<BitSet>[] codes = instances(2, HASH_SET_FACTORY);
			
			for (int i = 0; i < data.length; i += step) {
				for (int j = 0; j < inputDimension; ++j) {
					result.getNeuronValues()[1 + j] = data[i + j];
				}
				
				result.update(0);
				
				final BitSet code = new BitSet(layer1NeuronCount);
				
				for (int j = 1 + inputDimension; j < 1 + inputDimension + layer1NeuronCount; ++j) {
					if (0.0 < result.getNeuronValues()[j]) {
						code.set(j - (1 + inputDimension));
					}
				}
				
				codes[(int) data[i + step - 1]].add(code);
			}
			
			if (debug) {
				final Set<BitSet> ambiguities = new HashSet<BitSet>(codes[0]);
				
				ambiguities.retainAll(codes[1]);
				
				if (0 != ambiguities.size()) {
					debugPrint("ambiguityCount:", ambiguities.size());
					throw new IllegalStateException();
				}
			}
			
			result.newLayer();
			
			for (final BitSet code : codes[1]) {
				result.newNeuron(NEURON_TYPE_SUM_THRESHOLD);
				
				result.newNeuronSource(0, -code.cardinality());
				
				for (int i = 0; i < layer1NeuronCount; ++i) {
					result.newNeuronSource(1 + inputDimension + i, code.get(i) ? 1.0 : -1.0);
				}
			}
		}
		
		final int layer2NeuronCount = result.getLayerNeuronCount(2);
		
		if (debug) {
			debugPrint("layer2NeuronCount:", layer2NeuronCount);
		}
		
		// Build output layer (union of convexes)
		{
			result.newLayer();
			
			result.newNeuron(NEURON_TYPE_SUM_THRESHOLD);
			
			result.newNeuronSource(0, -0.5);
			
			for (int i = 0; i < layer2NeuronCount; ++i) {
				result.newNeuronSource(1 + inputDimension + layer1NeuronCount + i, 1.0);
			}
		}
		
		return result;
	}
	
	public static final List<Integer> range(final int first, final int last) {
		final List<Integer> result = new ArrayList<Integer>(last - first + 1);
		
		for (int i = first; i <= last; ++i) {
			result.add(i);
		}
		
		return result;
	}
	
	public static final Random RANDOM = new Random(0L);
	
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
	
	public static final void show(final FeedforwardNeuralNetwork network, final int imageSize, final double scale, final double[] trainingData) {
		final BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_3BYTE_BGR);
		
		draw(network, image, scale);
		
		if (trainingData != null) {
			final Graphics2D g = image.createGraphics();
			final int inputDimension = network.getLayerNeuronCount(0) - 1;
			
			for (int i = 0; i < trainingData.length; i += inputDimension + 1) {
				final double label = trainingData[i + inputDimension];
				
				if (label < 0.5) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.GREEN);
				}
				
				g.drawOval(
						imageSize / 2 + (int) (trainingData[i + 0] * scale) - 2,
						imageSize / 2 - (int) (trainingData[i + 1] * scale) - 2,
						4, 4);
			}
			
			g.dispose();
		}
		
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
	
}
