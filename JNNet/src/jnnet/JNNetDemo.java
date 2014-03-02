package jnnet;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.util.Collections.sort;
import static jnnet.ConstantValueSource.ONE;
import static net.sourceforge.aprog.tools.MathTools.Statistics.square;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jnnet.Neuron.Input;

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
	
	/**
	 * @param commandLineArguments
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final int w = 256;
		final int h = w;
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		final Graphics2D g = image.createGraphics();
		final TicToc timer = new TicToc();
		
		debugPrint(new Date(timer.tic()));
		
//		updateImage(network, image);
		
		debugPrint(timer.toc());
		
		final JLabel imageComponent = new JLabel(new ImageIcon(image));
		
		SwingTools.show(imageComponent, "ANN output", false);
		
		final List<TrainingItem> trainingItems = new ArrayList<TrainingItem>();
		final Random random = new Random(w + h);
		final int trainingSetIndex = 2;
		
		if (trainingSetIndex == 0) {
			final int k = 6;
			
			makeCircle(trainingItems, random, new double[] { 1.0 }, 50.0, k);
			makeCircle(trainingItems, random, new double[] { 0.0 }, 80.0, k);
			makeCircle(trainingItems, random, new double[] { 0.0 }, 30.0, k);
		} else if (trainingSetIndex == 1) {
			final double s = 50.0;
			final int k = 200;
			
			makeBox(trainingItems, random, new double[] { 1.0 }, +0.0, +0.0, +  s, +  s, k);
			makeBox(trainingItems, random, new double[] { 1.0 }, -  s, -  s, +0.0, +0.0, k);
			makeBox(trainingItems, random, new double[] { 0.0 }, -  s, +0.0, +0.0, +  s, k);
			makeBox(trainingItems, random, new double[] { 0.0 }, +0.0, -  s, +  s, +0.0, k);
		} else if (trainingSetIndex == 2) {
			final double scale = 20.0;
			final Scanner scanner = new Scanner(Tools.getResourceAsStream("jnnet/2spirals.txt"));
			
			while (scanner.hasNext()) {
				final Scanner line = new Scanner(scanner.nextLine());
				final double x = line.nextDouble() * scale;
				final double y = line.nextDouble() * scale;
				final double label = line.nextDouble();
				
				trainingItems.add(new TrainingItem(inputs(x, y), outputs(label == 1.0 ? 1.0 : 0.0)));
			}
			
			debugPrint(trainingItems.size());
		}
		
		final double scale = 5.0;
//		final Network network = newNetwork(scale, 2, 16, 1);
		final Network network = newNetwork(scale, 2, 48, 8, 1);
//		final Network network = newNetwork(scale, 2, 2, 2, 1);
//		
//		network.getNeurons().get(0).getInputs()[0].setWeight(+10.0);
//		network.getNeurons().get(0).getInputs()[1].setWeight(+0.0);
//		network.getNeurons().get(0).getInputs()[2].setWeight(+0.0);
//		network.getNeurons().get(1).getInputs()[0].setWeight(+0.0);
//		network.getNeurons().get(1).getInputs()[1].setWeight(+10.0);
//		network.getNeurons().get(1).getInputs()[2].setWeight(+0.0);
//		
//		network.getNeurons().get(2).getInputs()[0].setWeight(+ 6.0);
//		network.getNeurons().get(2).getInputs()[1].setWeight(+ 6.0);
//		network.getNeurons().get(2).getInputs()[2].setWeight(- 9.0);
//		network.getNeurons().get(3).getInputs()[0].setWeight(- 6.0);
//		network.getNeurons().get(3).getInputs()[1].setWeight(- 6.0);
//		network.getNeurons().get(3).getInputs()[2].setWeight(+ 3.0);
//		
//		network.getNeurons().get(4).getInputs()[0].setWeight(+10.0);
//		network.getNeurons().get(4).getInputs()[1].setWeight(+10.0);
//		network.getNeurons().get(4).getInputs()[2].setWeight(- 5.0);
		
		final NetworkEvaluator evaluator = new NetworkEvaluator(network, scale, trainingItems.toArray(new TrainingItem[0]));
		final EvolutionaryMinimizer minimizer = new EvolutionaryMinimizer(
				evaluator, 0, NetworkEvaluator.makeScale(network, scale));
		final int algo = 0;
		
		debugPrint("error:", evaluator.evaluate());
		
		evaluate(network, +20.0, +20.0);
		debugPrint(network);
		evaluate(network, -20.0, +20.0);
		debugPrint(network);
		evaluate(network, -20.0, -20.0);
		debugPrint(network);
		evaluate(network, +20.0, -20.0);
		debugPrint(network);
		
		timer.tic();
		
		double error = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < 400; ++i) {
			Tools.gc(20L);
			
			debugPrint("iteration:", i);
			
			if (algo == 0) {
				evaluator.train(0.1);
			} else {
				minimizer.update();
				
				NetworkEvaluator.setWeights(network, minimizer.getPopulation().get(0).getSample());
			}
			
			final double newError = evaluator.evaluate();
			
			debugPrint("totalTime:", timer.toc(), "error:", newError);
			
			drawOutputs(network, image);
//			drawFirstLayerNeurons(network, g, w, h);
			drawTrainingItems(trainingItems, g, w, h);
			
			SwingUtilities.invokeAndWait(new Runnable() {
				
				@Override
				public final void run() {
					imageComponent.setIcon(new ImageIcon(image));
					imageComponent.repaint();
				}
			});
			
			if (newError == 0.0 || newError - error == 0.0) {
				break;
			}
			
			error = newError;
		}
		
		debugPrint(network);
	}
	
	public static final double[] evaluate(final Network network, final double... inputs) {
		final int inputCount = network.getInputs().size();
		final int outputCount = network.getOutputs().size();
		final double[] result = new double[outputCount];
		
		for (int i = 0; i < inputCount; ++i) {
			network.getInputs().get(i).setValue(inputs[i]);
		}
		
		network.update();
		
		for (int i = 0; i < outputCount; ++i) {
			result[i] = network.getOutputs().get(i).getValue();
		}
		
		return result;
	}
	
	public static final double[] inputs(final double... values) {
		return values;
	}
	
	public static final double[] outputs(final double... values) {
		return values;
	}
	
	public static final void makeBox(final List<TrainingItem> trainingItems,
			final Random random, final double[] outputs,
			final double left, final double bottom, final double right, final double top,
			final int k) {
		final double w = right - left;
		final double h = top - bottom;
		
		for (int i = 0; i < k; ++i) {
			final double x = left + random.nextDouble() * w;
			final double y = bottom + random.nextDouble() * h;
			
			trainingItems.add(new TrainingItem(inputs(x, y), outputs));
		}
	}
	
	public static final void makeCircle(final List<TrainingItem> trainingItems,
			final Random random, final double[] outputs, final double radius,
			final int k) {
		final int n = (int) radius * k;
		
		for (int i = 0; i < n; ++i) {
			final double angle = random.nextDouble() * 2.0 * PI;
			final double r = radius * (1.0 + (random.nextDouble() - 0.5) / 4.0);
			
			trainingItems.add(new TrainingItem(new double[] { r * cos(angle), r * sin(angle) }, outputs));
		}
	}
	
	public static final void drawOutputs(final Network network, final BufferedImage image) {
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
	
	public static final void drawFirstLayerNeurons(final Network network, final Graphics g, final int w, final int h) {
		final double halfW = w / 2.0;
		final double halfH = h / 2.0;
		
		g.setColor(Color.BLUE);
		
		for (final Neuron neuron : network.getNeurons()) {
			if (neuron.getInputs()[0].getValueSource() instanceof Neuron) {
				break;
			}
			
			// a x + b y + c = 0
			final double a = neuron.getInputs()[0].getWeight();
			final double b = neuron.getInputs()[1].getWeight();
			final double c = neuron.getInputs()[2].getWeight();
			final double x1;
			final double y1;
			final double x2;
			final double y2;
			
			if (b != 0.0) {
				x1 = -halfW;
				y1 = (-c - a * x1) / b;
				x2 = +halfW;
				y2 = (-c - a * x2) / b;
			} else {
				y1 = -halfH;
				x1 = (-c - b * y1) / a;
				y2 = +halfH;
				x2 = (-c - b * y2) / a;
			}
			g.drawLine((int) (halfW + x1), (int) (halfH - y1), (int) (halfW + x2), (int) (halfH - y2));
		}
	}
	
	public static final void drawTrainingItems(final List<TrainingItem> trainingItems,
			final Graphics g, final int w, final int h) {
		for (final TrainingItem trainingItem : trainingItems) {
			final int x = (int) (w / 2.0 + trainingItem.getInputs()[0]);
			final int y = (int) (h / 2.0 - trainingItem.getInputs()[1]);
			
			g.setColor(trainingItem.getOutputs()[0] < 0.5 ? Color.RED : Color.GREEN);
			g.fillOval(x - 1, y - 1, 3, 3);
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
			
			final List<Neuron> layer = new ArrayList<Neuron>(layerSize);
			
			for (int j = 0; j < layerSize; ++j) {
				final Neuron neuron = new Neuron(new Input[sourceCount + 1]);
				
				for (int k = 0; k < sourceCount; ++k) {
					neuron.getInputs()[k] = new Input(
							sources.get(sources.size() - sourceCount + k), 2.0 * random.nextDouble() - 1.0);
				}
				
				neuron.getInputs()[sourceCount] = new Input(ONE, (2.0 * random.nextDouble() - 1.0) * scale);
				
				layer.add(neuron);
			}
			
			for (final Neuron neuron : layer) {
				if (i < layerCount - 1) {
					result.addNeuron(neuron);
				} else {
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
	public static final class NetworkEvaluator implements EvolutionaryMinimizer.Evaluator {
		
		private final Network network;
		
		private final TrainingItem[] trainingItems;
		
		private final double[] scale;
		
		private final Random random;
		
		public NetworkEvaluator(final Network network, final double scale, final TrainingItem... trainingItems) {
			this.network = network;
			this.trainingItems = trainingItems;
			this.scale = makeScale(network, scale);
			this.random = new Random(Arrays.hashCode(this.scale));
		}
		
		@Override
		public final double evaluate(final double[] sample) {
			setWeights(this.network, sample);
			
			return this.evaluate();
		}
		
		public final double evaluate() {
			double result = 0.0;
			
			for (final TrainingItem trainingItem : this.trainingItems) {
				trainingItem.setInputs(this.network);
				
				result += trainingItem.computeError(this.network);
			}
			
			return result / 2.0;
		}
		
		public final void train(final double deltaWeight) {
			final int algo = 1;
			double error = this.evaluate();
			int i = 0;
			
			if (algo == 0) {
				for (final Neuron neuron : this.network.getNeurons()) {
					for (final Input input : neuron.getInputs()) {
						final double dw = this.scale[i++] * (this.random.nextDouble() - 0.5) * error * deltaWeight;
						error = this.updateWeight(input, dw, error);
					}
				}
			} else if (algo == 1) {
				for (final Neuron neuron : this.network.getNeurons()) {
					final double norm = getNorm(neuron);
					final List<Input> orientation = new ArrayList<Input>();
					final List<Double> positionDw = new ArrayList<Double>();
					Input constantInput = null;
					double constantInputDw = 0.0;
					
					for (final Input input : neuron.getInputs()) {
						final double dw = this.scale[i++] * (this.random.nextDouble() - 0.5) * error * deltaWeight;
						
						if (input.getValueSource() instanceof ConstantValueSource) {
							constantInput = input;
							constantInputDw = dw;
						} else {
							orientation.add(input);
							positionDw.add(dw);
						}
					}
					
					if (constantInput != null) {
						error = this.updateWeight(constantInput, constantInputDw, error);
					}
					
					{
						int j = 0;
						
						for (final Input input : orientation) {
							error = this.updateWeight(input, positionDw.get(j++), error, orientation, norm);
						}
					}
					
					{
						final double scale = 1.0 + (this.random.nextDouble() - 0.5);
						
						scale(orientation, scale);
						
						double newError = this.evaluate();
						
						if (error <= newError) {
							scale(orientation, 1 / square(scale));
							
							newError = this.evaluate();
							
							if (error <= newError) {
								scale(orientation, scale);
							} else {
								error = newError;
							}
						}
					}
				}
			} else {
				throw new IllegalArgumentException();
			}
		}
		
		public static final void scale(final Iterable<Input> inputs, final double scale) {
			if (!Double.isNaN(scale) && !Double.isInfinite(scale)) {
				for (final Input input : inputs) {
					input.setWeight(input.getWeight() * scale);
				}
			}
		}
		
		public final double updateWeight(final Input input, final double dw,
				final double error) {
			final double weight = input.getWeight();
			
			input.setWeight(weight + dw);
			
			double newError = this.evaluate();
			
			if (error < newError) {
				input.setWeight(weight - dw);
				newError = this.evaluate();
			}
			
			if (newError < error) {
				return newError;
			}
			
			input.setWeight(weight);
			
			return error;
		}
		
		public final double updateWeight(final Input input, final double dw,
				final double error, final Iterable<Input> orientation, final double norm) {
			final double weight = input.getWeight();
			final List<Double> initialWeights = new ArrayList<Double>();
			
			for (final Input i : orientation) {
				initialWeights.add(i.getWeight());
			}
			
			input.setWeight(weight + dw);
			scale(orientation, norm / getNorm(orientation));
			
			double newError = this.evaluate();
			
			if (error < newError) {
				int j = 0;
				
				for (final Input i : orientation) {
					i.setWeight(initialWeights.get(j++));
				}
				
				input.setWeight(weight - dw);
				scale(orientation, norm / getNorm(orientation));
				newError = this.evaluate();
			}
			
			if (newError < error) {
				return newError;
			}
			
			{
				int j = 0;
				
				for (final Input i : orientation) {
					i.setWeight(initialWeights.get(j++));
				}
			}
			
			return error;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4159192974740277150L;
		
		public static final double getNorm(final Neuron neuron) {
			double norm2 = 0.0;
			
			for (final Input input : neuron.getInputs()) {
				if (!(input.getValueSource() instanceof ConstantValueSource)) {
					norm2 += square(input.getWeight());
				}
			}
			
			return sqrt(norm2);
		}
		
		public static final double getNorm(final Iterable<Input> inputs) {
			double norm2 = 0.0;
			
			for (final Input input : inputs) {
				norm2 += square(input.getWeight());
			}
			
			return sqrt(norm2);
		}
		
		public static final void setWeights(final Network network, final double[] weights) {
			int i = 0;
			
			for (final Neuron neuron : network.getNeurons()) {
				for (final Input input : neuron.getInputs()) {
					input.setWeight(weights[i++]);
				}
			}
		}
		
		public static final double[] makeScale(final Network network, final double scale) {
			final List<Double> protoresult = new ArrayList<Double>();
			
			for (final Neuron neuron : network.getNeurons()) {
				for (final Input input : neuron.getInputs()) {
					if (!(input.getValueSource() instanceof ConstantValueSource)) {
						protoresult.add(-1.0);
					} else {
						protoresult.add(-scale);
					}
				}
			}
			
			final int n = protoresult.size();
			final double[] result = new double[n];
			
			for (int i = 0; i < n; ++i) {
				result[i] = protoresult.get(i);
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-17)
	 */
	public static final class EvolutionaryMinimizer implements Serializable {
		
		private final Random random;
		
		private final List<ValuedSample> population;
		
		private final double[] scale;
		
		private final Evaluator evaluator;
		
		public EvolutionaryMinimizer(final Evaluator evaluator, final int populationSize, final double... scale) {
			this.random = new Random(populationSize + Arrays.hashCode(scale));
			this.population = new ArrayList<ValuedSample>(populationSize);
			this.scale = scale;
			this.evaluator = evaluator;
			
			for (int i = 0; i < populationSize; ++i) {
				this.getPopulation().add(this.new ValuedSample(this.randomize(null)).updateValue());
			}
			
			sort(this.getPopulation());
		}
		
		public final double[] randomize(final double[] sample) {
			final int n = this.scale.length;
			final double[] result = sample != null ? sample : new double[n];
			
			for (int i = 0; i < n; ++i) {
				final double scale = this.scale[i];
				final double random = this.random.nextDouble();
				result[i] = scale * (scale < 0.0 ? 2.0 * random - 1.0 : random);
			}
			
			return result;
		}
		
		public final void update() {
			final int populationSize = this.getPopulation().size();
			
			this.randomize(this.getPopulation().get(populationSize - 1).getSample());
			
			for (int i = populationSize - 1; 1 <= i; --i) {
				this.getPopulation().get(i).mergeWith(this.getPopulation().get((i + 1) % populationSize).getSample());
			}
			
			sort(this.getPopulation());
		}
		
		public final Evaluator getEvaluator() {
			return this.evaluator;
		}
		
		public final List<ValuedSample> getPopulation() {
			return this.population;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -763903170794513358L;
		
		/**
		 * @author codistmonk (creation 2013-12-17)
		 */
		public static abstract interface Evaluator extends Serializable {
			
			public abstract double evaluate(final double[] sample);
			
		}
		
		/**
		 * @author codistmonk (creation 2013-12-17)
		 */
		public final class ValuedSample implements Serializable, Comparable<ValuedSample> {
			
			private final double[] sample;
			
			private double value;
			
			public ValuedSample(final double[] sample) {
				this.sample = sample;
			}
			
			public final double[] getSample() {
				return this.sample;
			}
			
			public final double getValue() {
				return this.value;
			}
			
			public final ValuedSample updateValue() {
				this.value = EvolutionaryMinimizer.this.getEvaluator().evaluate(this.getSample());
				
				return this;
			}
			
			public final void mergeWith(final double[] sample) {
				final int n = sample.length;
				final double preservation = 0.5;
				final double renewal = 1.0 - preservation;
				
				for (int i = 0; i < n; ++i) {
					this.getSample()[i] = this.getSample()[i] * preservation + sample[i] * renewal;
				}
				
				this.updateValue();
			}
			
			@Override
			public final int compareTo(final ValuedSample that) {
				return Double.compare(this.getValue(), that.getValue());
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -1359740563674338330L;
			
		}
		
	}
	
}
