package jnnet3.dct;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static jnnet3.dct.Expressor.express;
import static multij.tools.Tools.*;

import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-03-25)
 */
public final class ASMDemo {
	
	private ASMDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		if (true) {
			new ClassReader(ASMDemo.class.getName()).accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
		}
		if (true) {
			debugPrint(express(ASMDemo.class.getName(), "f"));
		}
		if (true) {
			debugPrint(express(ASMDemo.class.getName(), "dct1k"));
		}
		if (true) {
			debugPrint(express(ASMDemo.class.getName(), "idct1k"));
		}
	}
	
	public static final double f(final double x, final double y) {
		final double a = x * y;
		
		return a + cos(x / sqrt(2.0));
	}
	
	public static final double dct1k(final double a, final double x, final double k, final double n) {
		return a * cos(((2.0 * x) + 1.0) * k * PI / 2.0 / n);
	}
	
	public static final double idct1k(final double a, final double x, final double k, final double n) {
		return dct1k(a, x, k, n) * sqrt(2.0);
	}
	
}
