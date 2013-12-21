package jnnet3;

import static jnnet3.JNNetTools.draw;
import static jnnet3.JNNetTools.inputs;
import static jnnet3.JNNetTools.outputs;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.image.BufferedImage;

import jnnet3.ArtificialNeuralNetwork.Training;
import jnnet3.ArtificialNeuralNetwork.Training.Item;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-12-20)
 */
public final class JNNetDemo {
	
	private JNNetDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final ArtificialNeuralNetwork ann = new ArtificialNeuralNetwork(2);
		ann.addOutputNeuron(1, -30.0, 0.4, 0.1);
		debugPrint(ann);
		final Training training = new Training(
				new Item(inputs(40.0, 40.0), outputs(0.0)),
				new Item(inputs(80.0, 80.0), outputs(1.0))
		);
		
		{
			final int w = 256;
			final int h = w;
			
			{
				final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				
				draw(ann, image);
				draw(training, image);
				
				SwingTools.show(image, "ANN output before training", false);
			}
			
			for (int i = 0; i < 10000; ++i) {
				training.train(ann, 1.0);
				debugPrint(i, ann.evaluate(50.0, 50.0).getOutputValue(0), ann.evaluate(70.0, 70.0).getOutputValue(0));
			}
			debugPrint(ann);
			
			{
				final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				
				draw(ann, image);
				draw(training, image);
				
				SwingTools.show(image, "ANN output after training", false);
			}
		}
	}
	
}
