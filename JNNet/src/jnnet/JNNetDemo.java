package jnnet;

import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
	
	public static final ConstantValueSource ONE = new ConstantValueSource(1.0);
	
	/**
	 * @param commandLineArguments
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final Network network = newNetwork(5.0, 2, 3, 1);
		
		final int w = 256;
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
		
		for (int i = 0; i < 200; ++i) {
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
					neuron.getInputs()[k] = new Input(
							sources.get(sources.size() - sourceCount + k), 2.0 * random.nextDouble() - 1.0);
				}
				
				neuron.getInputs()[sourceCount] = new Input(ONE, (2.0 * random.nextDouble() - 1.0) * scale);
				
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
	
}
