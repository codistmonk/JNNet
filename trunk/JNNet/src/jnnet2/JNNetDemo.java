package jnnet2;

import static jnnet2.JNNetTools.draw;
import static jnnet2.JNNetTools.inputs;
import static jnnet2.JNNetTools.outputs;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.image.BufferedImage;

import jnnet2.ArtificialNeuralNetwork.Training;
import jnnet2.ArtificialNeuralNetwork.Training.Item;

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
		ann.addOutputNeuron(1, 0.0, 0.002, 0.010);
		debugPrint(ann);
		final Training training = new Training(
				new Item(inputs(50.0, 50.0), outputs(1.0))
		);
		
		{
			final int w = 256;
			final int h = w;
			final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			
			draw(ann, image);
			draw(training, image);
			
			SwingTools.show(image, "ANN output", false);
		}
	}
	
}
