package jnnet;

import static java.lang.Math.exp;
import static net.sourceforge.aprog.tools.MathTools.Statistics.square;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jnnet.JNNetDemo.Neuron.Input;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-12-14)
 */
public final class JNNetDemo {
	
	private JNNetDemo() {
		throw new IllegalInstantiationException();
	}
	
	public static final ConstantValueSource ONE = new ConstantValueSource(1.0);
	
	/**
	 * @param commandLineArguments
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
//		final Network network = new Network();
//		final ModifiableValueSource xSource = new ModifiableValueSource();
//		final ModifiableValueSource ySource = new ModifiableValueSource();
//		
//		network.addInput(xSource);
//		network.addInput(ySource);
////		network.addNeuron(halfspace(xSource, ySource, -10.0, +10.0, +1.0, -1.0));
////		network.addNeuron(halfspace(xSource, ySource, +10.0, +10.0, -1.0, -1.0));
////		network.addNeuron(halfspace(xSource, ySource, + 0.0, -10.0, +0.0, +1.0));
////		network.addOutput(combineLast(network.getNeurons(), 1.0, 1.0, 1.0));
//		network.addNeuron(halfspace(xSource, ySource, -10.0, +10.0, +0.30, +0.03));
//		network.addNeuron(halfspace(xSource, ySource, +10.0, +10.0, -0.92, +0.91));
//		network.addOutput(combineLast(network.getNeurons(), +0.26, -0.96));
		final Network network = newNetwork(10.0, 2, 3, 1);
		
		final int w = 512;
		final int h = w;
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		final Graphics2D g = image.createGraphics();
		final TicToc timer = new TicToc();
		
		debugPrint(new Date(timer.tic()));
		
		updateImage(network, image);
		
		debugPrint(timer.toc());
		
		final JLabel imageComponent = new JLabel(new ImageIcon(image));
		
		SwingTools.show(imageComponent, "ANN output", false);
		
		final TrainingItem[] trainingItems = {
				new TrainingItem(new double[] { -15.0, -15.0 }, new double[] { 1.0 }),
				new TrainingItem(new double[] { +15.0, -15.0 }, new double[] { 1.0 }),
				new TrainingItem(new double[] { + 0.0, +15.0 }, new double[] { 1.0 }),
				new TrainingItem(new double[] { -90.0, -90.0 }, new double[] { 0.0 }),
				new TrainingItem(new double[] { +90.0, -90.0 }, new double[] { 0.0 }),
				new TrainingItem(new double[] { + 0.0, +90.0 }, new double[] { 0.0 }),
		};
		
		for (int i = 0; i < 2000; ++i) {
			Tools.gc(20L);
			
			debugPrint(i);
			
			for (final TrainingItem trainingItem : trainingItems) {
				trainingItem.train(network, 0.01);
			}
			
			updateImage(network, image);
			
			for (final TrainingItem trainingItem : trainingItems) {
				final int x = (int) (w / 2.0 + trainingItem.getInputs()[0]);
				final int y = (int) (h / 2.0 - trainingItem.getInputs()[1]);
				
				g.setColor(trainingItem.getOutputs()[0] < 0.5 ? Color.RED : Color.GREEN);
				g.fillOval(x - 1, y - 1, 3, 3);
			}
			
			SwingUtilities.invokeAndWait(new Runnable() {
				
				@Override
				public final void run() {
					imageComponent.setIcon(new ImageIcon(image));
					imageComponent.repaint();
				}
			});
		}
	}

	public static void updateImage(final Network network, final BufferedImage image) {
		final ModifiableValueSource xSource = network.getInputs().get(0);
		final ModifiableValueSource ySource = network.getInputs().get(1);
		final Neuron output = network.getOutputs().get(0);
		final int w = image.getWidth();
		final int h = image.getHeight();
		
		for (int y = 0; y < h; ++y) {
			ySource.setValue(h / 2.0 - y);
			
			for (int x = 0; x < w; ++x) {
				xSource.setValue(x - w / 2.0);
				
				network.update();
				
				image.setRGB(x, y, 0xFF000000 | (0x00010101 * (int) (output.getValue() * 255.0)));
			}
		}
	}
	
	public static final void updateNeurons(final Iterable<Neuron> neurons) {
		for (final Neuron neuron : neurons) {
			neuron.update();
		}
	}
	
	public static final Neuron combineLast(final List<Neuron> network, final double... weights) {
		final int neuronCount = network.size();
		final int lastCount = weights.length;
		final Input[] inputs = new Input[lastCount];
		
		for (int i = 0; i < lastCount; ++i) {
			inputs[i] = new Input(network.get(neuronCount - lastCount + i), weights[i]);
		}
		
		return new Neuron(inputs);
	}
	
	public static final Network newNetwork(final double scale, final int inputCount, final int... layers) {
		final Network result = new Network();
		final Random random = new Random(inputCount + Arrays.hashCode(layers));
		
		for (int i = 0; i < inputCount; ++i) {
			result.getInputs().add(new ModifiableValueSource());
		}
		
		final int layerCount = layers.length;
		
		for (int i = 0; i < layerCount; ++i) {
			final int layerSize = layers[i];
			final List<? extends ValueSource> sources;
			final int sourceCount;
			
			if (i == 0) {
				sources = result.getInputs();
				sourceCount = inputCount;
			} else {
				sources = result.getNeurons();
				sourceCount = layers[i - 1];
			}
			
			final List<Neuron> layer = new ArrayList<>(layerSize);
			
			for (int j = 0; j < layerSize; ++j) {
				final Neuron neuron = new Neuron(new Input[sourceCount + 1]);
				
				for (int k = 0; k < sourceCount; ++k) {
					neuron.getInputs()[k] = new Input(sources.get(sources.size() - sourceCount + k), 2.0 * random.nextDouble() - 1.0);
					debugPrint(neuron.getInputs()[k].getWeight());
				}
				
				neuron.getInputs()[sourceCount] = new Input(ONE, (2.0 * random.nextDouble() - 1.0) * scale);
				debugPrint(neuron.getInputs()[sourceCount].getWeight());
				
				layer.add(neuron);
			}
			
			for (final Neuron neuron : layer) {
				if (i < layerCount - 1) {
					debugPrint();
					result.addNeuron(neuron);
				} else {
					debugPrint();
					result.addOutput(neuron);
				}
			}
		}
		
		return result;
	}
	
	public static final Neuron halfspace(final ValueSource xSource, final ValueSource ySource,
			final double locationX, final double locationY, final double directionX, final double directionY) {
		return new Neuron(
				new Input(ONE, - (locationX * directionX + locationY * directionY)),
				new Input(xSource, directionX),
				new Input(ySource, directionY));
	}
	
	public static final <T> T get(final List<T> list, final int index) {
		return 0 <= index ? list.get(index) : list.get(index + list.size());
	}
	
	/**
	 * @author codistmonk (creation 2013-12-17)
	 */
	public static final class TrainingItem implements Serializable {
		
		private final double[] inputs;
		
		private final double[] outputs;
		
		public TrainingItem(final double[] inputs, final double[] outputs) {
			this.inputs = inputs;
			this.outputs = outputs;
		}
		
		public final double[] getInputs() {
			return this.inputs;
		}
		
		public final double[] getOutputs() {
			return this.outputs;
		}
		
		public final void train(final Network network, final double weightDelta) {
			this.setInputs(network);
			
			double error = this.computeError(network);
			
			for (final Neuron neuron : network.getNeurons()) {
				for (final Input input : neuron.getInputs()) {
					final double weight = input.getWeight();
					
					input.setWeight(weight + weightDelta * error);
					
					double newError = this.computeError(network);
					
//					debugPrint(Arrays.toString(this.getInputs()), Arrays.toString(this.getOutputs()), network.getOutputs().get(0).getValue());
//					debugPrint(error, newError);
					
					if (error < newError) {
						input.setWeight(weight - weightDelta * error);
						newError = this.computeError(network);
					}
					
					if (error <= newError) {
						input.setWeight(weight);
					} else {
						error = newError;
					}
				}
			}
		}
		
		public final void setInputs(final Network network) {
			final int n = this.getInputs().length;
			
			for (int i = 0; i < n; ++i) {
				network.getInputs().get(i).setValue(this.getInputs()[i]);
			}
		}
		
		public final double computeError(final Network network) {
			network.update();
			
			final int n = this.getOutputs().length;
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				result += square(network.getOutputs().get(i).getValue() - this.getOutputs()[i]);
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 7239242132428142945L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-17)
	 */
	public static final class Network implements Serializable {
		
		private final List<ModifiableValueSource> inputs;
		
		private final List<Neuron> neurons;
		
		private final List<Neuron> outputs;
		
		public Network() {
			this.inputs = new ArrayList<>();
			this.neurons = new ArrayList<>();
			this.outputs = new ArrayList<>();
		}
		
		public final List<ModifiableValueSource> getInputs() {
			return this.inputs;
		}
		
		public final List<Neuron> getNeurons() {
			return this.neurons;
		}
		
		public final List<Neuron> getOutputs() {
			return this.outputs;
		}
		
		public final void update() {
			updateNeurons(this.getNeurons());
		}
		
		public final Network addInput(final ModifiableValueSource input) {
			this.getInputs().add(input);
			
			return this;
		}
		
		public final Network addNeuron(final Neuron neuron) {
			this.getNeurons().add(neuron);
			
			return this;
		}
		
		public final Network addOutput(final Neuron neuron) {
			this.getOutputs().add(neuron);
			
			return this.addNeuron(neuron);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -2125463032324232908L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static abstract interface ValueSource extends Serializable {
		
		public abstract void update();
		
		public abstract double getValue();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class ConstantValueSource implements ValueSource {
		
		private final double value;
		
		public ConstantValueSource(final double value) {
			this.value = value;
		}
		
		@Override
		public final void update() {
			// NOP
		}
		
		@Override
		public final double getValue() {
			return this.value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 5846379572555159967L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class ModifiableValueSource implements ValueSource {
		
		private double value;
		
		@Override
		public final void update() {
			// NOP
		}
		
		@Override
		public final double getValue() {
			return this.value;
		}
		
		public final void setValue(final double value) {
			this.value = value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -4620991329089575653L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class Neuron implements ValueSource {
		
		private final Input[] inputs;
		
		private double value;
		
		public Neuron(final Input... inputs) {
			this.inputs = inputs;
		}
		
		public final Input[] getInputs() {
			return this.inputs;
		}
		
		@Override
		public final void update() {
			this.value = 0.0;
			
			for (final Input input : this.getInputs()) {
				this.value += input.getValue();
			}
			
			this.value = sigmoid(this.value);
		}
		
		@Override
		public final double getValue() {
			return this.value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 966129720042922563L;
		
		public static final double sigmoid(final double x) {
			return 1.0 / (1.0 + exp(-x));
		}
		
		/**
		 * @author codistmonk (creation 2013-12-14)
		 */
		public static final class Input implements ValueSource {
			
			private final ValueSource valueSource;
			
			private double weight;
			
			public Input(final ValueSource valueSource, final double weight) {
				this.valueSource = valueSource;
				this.weight = weight;
			}
			
			public final ValueSource getValueSource() {
				return this.valueSource;
			}
			
			public final double getWeight() {
				return this.weight;
			}
			
			public final void setWeight(final double weight) {
				this.weight = weight;
			}
			
			@Override
			public final void update() {
				this.getValueSource().update();
			}
			
			@Override
			public final double getValue() {
				return this.getValueSource().getValue() * this.getWeight();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -6771686408291762051L;
			
		}
		
	}
	
}
