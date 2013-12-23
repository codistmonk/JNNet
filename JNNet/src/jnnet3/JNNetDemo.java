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
		final ArtificialNeuralNetwork ann = JNNetTools.newNetwork(2, 2, 1);
		debugPrint(ann);
		final Training training = new Training(
				new Item(inputs(40.0, 40.0), outputs(0.0)),
				new Item(inputs(80.0, 40.0), outputs(1.0)),
				new Item(inputs(80.0, 80.0), outputs(0.0)),
				new Item(inputs(40.0, 80.0), outputs(1.0))
		);
		
		training.initializeWeights(ann);
		
		debugPrint(ann);
		
		{
			final int w = 256;
			final int h = w;
			
			{
				final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				
				draw(ann, image);
				draw(training, image);
				
				SwingTools.show(image, "ANN output before training", false);
			}
			
			for (int i = 0; i < 500; ++i) {
				training.train(ann, 0.5);
				debugPrint(i, training.computeError(ann));
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
