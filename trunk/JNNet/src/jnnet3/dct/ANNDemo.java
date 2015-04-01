package jnnet3.dct;

import static jnnet3.dct.ANN.newIDCTNetwork;
import static jnnet3.dct.DCT.fullDCT;
import static jnnet3.dct.MiniCAS.constants;
import static net.sourceforge.aprog.tools.Tools.*;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import jnnet3.dct.ANN.Layer;
import jnnet3.dct.MiniCAS.Expression;

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
		final int dimensions = 3;
		
		if (dimensions == 1) {
			final Expression[] fullDCT = fullDCT(constants(1, 2, 3, 4));
			final ANN ann = newIDCTNetwork(fullDCT, 3);
			
			print(ann);
			
			for (double x = 0.0; x < 4; x += 0.5) {
				debugPrint("input:", x, "output:", Arrays.toString(ann.evaluate(x)));
			}
		} else if (dimensions == 2) {
			final Object fullDCT = fullDCT(array(constants(1, 2), constants(3, 4)));
			final ANN ann = newIDCTNetwork(fullDCT, 1);
			
			print(ann);
			
			for (double x1 = 0.0; x1 < 2; ++x1) {
				for (double x2 = 0.0; x2 < 2; ++x2) {
					debugPrint("input:", x1, x2, "output:", Arrays.toString(ann.evaluate(x1, x2)));
				}
			}
		} else if (dimensions == 3) {
			final Object fullDCT = fullDCT(array(
					array(constants(1, 2, 3, 4), constants(3, 4, 5, 6)),
					array(constants(5, 6, 7, 8), constants(7, 8, 9, 10))));
			final ANN ann = newIDCTNetwork(fullDCT, 4);
			
			print(ann);
			
			for (double x1 = 0.0; x1 < 2; ++x1) {
				for (double x2 = 0.0; x2 < 2; ++x2) {
					for (double x3 = 0.0; x3 < 4; ++x3) {
						debugPrint("input:", x1, x2, x3, "output:", Arrays.toString(ann.evaluate(x1, x2, x3)));
					}
				}
			}
		}
	}
	
	public static final void print(final ANN ann) {
		final List<Layer> layers = ann.getLayers();
		final int n = layers.size();
		
		for (int i = 0; i < n; ++i) {
			final Layer layer = layers.get(i);
			
			debugPrint("layer:", i, "neurons:", layer.getNeurons().size());
			
			if (false) {
				for (final double[] neuron : layer.getNeurons()) {
					debugPrint(Arrays.toString(neuron));
				}
			}
		}
	}
	
}
