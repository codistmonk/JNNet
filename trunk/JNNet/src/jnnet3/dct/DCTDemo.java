package jnnet3.dct;

import static jnnet3.dct.DCT.*;
import static jnnet3.dct.MiniCAS.*;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.util.Arrays;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-01)
 */
public final class DCTDemo {
	
	private DCTDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		if (true) {
			final Expression[] f = expressions(121, 58);
			final Expression[] dct = deepApply(MiniCAS::constant, fullDCT(f));
			final Expression[] g = constants(idct(dct, 0), idct(dct, 1));
			
			debugPrint();
			debugPrint((Object[]) f);
			debugPrint((Object[]) dct);
			debugPrint((Object[]) g);
		}
		
		if (true) {
			final Expression[][] f = {
					expressions(1, 2),
					expressions(3, 4),
			};
			final Expression[][] dct = deepApply(MiniCAS::constant, fullDCT(f));
			final Expression[][] g = {
					constants(idct(dct, 0, 0), idct(dct, 0, 1)),
					constants(idct(dct, 1, 0), idct(dct, 1, 1)),
			};
			
			debugPrint();
			debugPrint(Arrays.deepToString(f));
			debugPrint(Arrays.deepToString(dct));
			debugPrint(Arrays.deepToString(g));
		}
		
		if (true) {
			final Expression[][][] f = {
					{
						constants(1.0, 2.0),
						constants(3.0, 6.0),
					},
					{
						constants(5.0, 4.0),
						constants(7.0, 8.0),
					},
			};
			final Expression[][][] dct = deepApply(MiniCAS::constant, fullDCT(f));
			final Expression[][][] g = {
					{
						constants(idct(dct, 0, 0, 0), idct(dct, 0, 0, 1)),
						constants(idct(dct, 0, 1, 0), idct(dct, 0, 1, 1)),
					},
					{
						constants(idct(dct, 1, 0, 0), idct(dct, 1, 0, 1)),
						constants(idct(dct, 1, 1, 0), idct(dct, 1, 1, 1)),
					},
			};
			
			debugPrint();
			debugPrint(Arrays.deepToString(f));
			debugPrint(Arrays.deepToString(dct));
			debugPrint(Arrays.deepToString(g));
			debugPrint(Arrays.deepToString(deepApply(MiniCAS::constant, fullIDCT(dct))));
			
			final Variable x1 = variable("x1");
			final Variable x2 = variable("x2");
			final Variable x3 = variable("x3");
			final Expression expression = approximate(separateCosProducts(idct(dct, x1, x2, x3)), 1.0E-8);
			
			x1.setValue(expression(0));
			x2.setValue(expression(1));
			x3.setValue(expression(1));
			
			debugPrint(expression.getAsDouble());
			
			if (true) {
				debugPrint();
				((Sum) expression).getOperands().forEach(System.out::println);
			}
		}
	}

}
