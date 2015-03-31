package dct;

import static dct.DCT.*;
import static dct.MiniCAS.*;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import net.sourceforge.aprog.tools.Tools;
import dct.MiniCAS.Constant;
import dct.MiniCAS.Expression;

/**
 * @author codistmonk (creation 2015-03-30)
 */
public final class ANN implements Serializable {
	
	private final int inputDimensions;
	
	private final List<Layer> layers;
	
	public ANN(final int inputDimensions) {
		this.inputDimensions = inputDimensions;
		this.layers = new ArrayList<>();
	}
	
	public final Layer addLayer(final DoubleUnaryOperator activation) {
		final int previousDimensions = this.getLayers().isEmpty() ? this.getInputDimensions() : getOutputLayer().getInputDimensions();
		final Layer result = new Layer(previousDimensions, activation);
		
		this.getLayers().add(result);
		
		return result;
	}
	
	public final double[] evaluate(final double... input) {
		double[] actualInput = input;
		double[] result = null;
		
		for (final Layer layer : this.getLayers()) {
			final int i = layer.getInputDimensions();
			
			if (actualInput.length + 1 == i) {
				actualInput = Arrays.copyOf(actualInput, i);
				actualInput[i - 1] = 1.0;
			}
			
			final int o = layer.getNeurons().size();
			
			if (result == null || result.length != o) {
				result = new double[o];
			}
		}
		
		return result;
	}
	
	public final Layer getOutputLayer() {
		return last(this.getLayers());
	}
	
	public final int getInputDimensions() {
		return this.inputDimensions;
	}
	
	public final List<Layer> getLayers() {
		return this.layers;
	}
	
	private static final long serialVersionUID = -7594919042796851934L;
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final ANN ann = newIDCTNetwork(constants(1, 2, 3, 4));
		
		// TODO Auto-generated method stub
	}
	
	public static final ANN newIDCTNetwork(final Object dct) {
		final int n = DCT.getDimensionCount(dct.getClass());
		final Object[] input = new Expression[n];
		
		for (int i = 0; i < n; ++i) {
			input[i] = variable("x" + i);
		}
		
		final ANN result = new ANN(n);
		final Expression idct = approximate(separateCosProducts(idct(dct, input)), 1.0E-8);
		Expression bias = ZERO;
		final List<Expression> terms = idct instanceof Sum ? ((Sum) idct).getOperands() : Arrays.asList(idct);
		
		for (final Expression term : terms) {
			Constant magnitude = ONE;
			Cos cos = cast(Cos.class, term);
			final Product product = cast(Product.class, term);
			
			if (product != null && product.getOperands().size() == 2) {
				magnitude = (Constant) product.getOperands().get(0);
				cos = (Cos) product.getOperands().get(1);
			} else if (cos == null) {
				if (!(term instanceof Constant)) {
					debugError("Unexpected term:", term);
				}
				
				bias = add(bias, term);
			}
			
			if (cos != null) {
				Tools.debugPrint(magnitude, cos);
			}
		}
		
		{
			result.addLayer(ANN::identity).addNeuron()[n - 1] = bias.getAsDouble();
			
			return result;
		}
	}
	
	public static final double identity(final double x) {
		return x;
	}
	
	public static final double sinmoid(final double x) {
		if (x < -Math.PI / 2.0) {
			return -1.0;
		}
		
		if (x <= Math.PI / 2.0) {
			return Math.sin(x);
		}
		
		return 1.0;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-30)
	 */
	public static final class Layer implements Serializable {
		
		private final int inputDimensions;
		
		private final DoubleUnaryOperator activation;
		
		private final List<double[]> neurons;
		
		public Layer(final int inputDimensions, final DoubleUnaryOperator activation) {
			this.inputDimensions = inputDimensions;
			this.activation = activation;
			this.neurons = new ArrayList<>();
		}
		
		public final double[] evaluate(final double[] input, final double[] result) {
			fill(result, 0.0);
			
			final int n = this.getNeurons().size();
			
			for (int i = 0; i < n; ++i) {
				result[i] = this.getActivation().applyAsDouble(dot(this.getNeurons().get(i), input));
			}
			
			return result;
		}
		
		public final double[] addNeuron() {
			final double[] result = new double[this.getInputDimensions()];
			
			this.getNeurons().add(result);
			
			return result;
		}
		
		public final int getInputDimensions() {
			return this.inputDimensions;
		}
		
		public final DoubleUnaryOperator getActivation() {
			return this.activation;
		}
		
		public final List<double[]> getNeurons() {
			return this.neurons;
		}
		
		private static final long serialVersionUID = -873786456456033793L;
		
		public static final double dot(final double[] v1, final double[] v2) {
			final int n = v1.length;
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				result += v1[i] * v2[i];
			}
			
			return result;
		}
		
	}
	
}
