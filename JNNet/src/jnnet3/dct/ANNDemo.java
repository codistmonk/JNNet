package jnnet3.dct;

import static jnnet3.dct.DCT.fullDCT;
import static jnnet3.dct.MiniCAS.constants;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.util.Arrays;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import jnnet3.dct.ANN.Layer;

/**
 * @author codistmonk (creation 2015-04-01)
 */
public final class ANNDemo {
	
	private ANNDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
//		final ANN ann = newIDCTNetwork(fullDCT(constants(1, 2, 3, 4)));
//		final Object fullDCT = fullDCT(array(constants(1, 2), constants(3, 4)));
		final Object fullDCT = fullDCT(array(array(constants(1, 2, 3, 4), constants(3, 4, 5, 6)), array(constants(5, 6, 7, 8), constants(7, 8, 9, 10))));
		final ANN ann = ANN.newIDCTNetwork(fullDCT, 4);
		
		for (final Layer layer : ann.getLayers()) {
			debugPrint(layer.getNeurons().size(), layer.getActivation());
			
			if (false) {
				for (final double[] neuron : layer.getNeurons()) {
					debugPrint(Arrays.toString(neuron));
				}
			}
		}
		
//		for (double x = 0.0; x < 4; x += 0.5) {
//			debugPrint(x, Arrays.toString(ann.evaluate(x)));
//		}
//		for (double x1 = 0.0; x1 < 2; ++x1) {
//			for (double x2 = 0.0; x2 < 2; ++x2) {
//				debugPrint(x1, x2, Arrays.toString(ann.evaluate(x1, x2)));
//			}
//		}
		for (double x1 = 0.0; x1 < 2; ++x1) {
			for (double x2 = 0.0; x2 < 2; ++x2) {
				for (double x3 = 0.0; x3 < 2; ++x3) {
					debugPrint(x1, x2, x3, Arrays.toString(ann.evaluate(x1, x2, x3)));
				}
			}
		}
	}
	
}
