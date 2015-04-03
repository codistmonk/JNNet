package jnnet3.dct;

import static java.util.Arrays.fill;
import static jnnet3.dct.DCT.*;
import static jnnet3.dct.MiniCAS.*;
import static net.sourceforge.aprog.tools.Tools.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

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
		final int previousDimensions = this.getLayers().isEmpty() ? this.getInputDimensions() + 1 : getOutputLayer().getNeurons().size() + 1;
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
			
			layer.evaluate(actualInput, result);
			
			actualInput = result;
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
	
	static final double EPSILON = 1.0E-12;
	
	static final DoubleUnaryOperator COS = Math::cos;
	
	static final DoubleUnaryOperator SINMOID = ANN::sinmoid;
	
	static final DoubleUnaryOperator IDENTITY = ANN::identity;
	
	public static final ANN newIDCTNetwork(final Object dct, final int cosApproximationQuality) {
		final int n = DCT.getDimensionCount(dct.getClass());
		final Object[] input = new Expression[n];
		
		for (int i = 0; i < n; ++i) {
			input[i] = variable("x" + i);
		}
		
		final ANN result = new ANN(n);
		final Expression idct = separateCosProducts(idct(dct, input), EPSILON);
		Expression bias = ZERO;
		final List<Expression> terms = idct instanceof Sum ? ((Sum) idct).getOperands() : Arrays.asList(idct);
		final Map<Variable, Constant> weights = new HashMap<>();
		Layer hiddenLayer = null;
		final List<Double> magnitudes = new ArrayList<>();
		final DoubleUnaryOperator activation = cosApproximationQuality < 0 ? COS : SINMOID;
		
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
				
				bias = approximate(add(bias, term), EPSILON);
				
				continue;
			}
			
			final Expression argument = cos.getOperand();
			final List<Expression> argumentTerms = argument instanceof Sum ? ((Sum) argument).getOperands() : Arrays.asList(argument);
			
			weights.clear();
			
			for (final Expression argumentTerm : argumentTerms) {
				Variable variable = cast(Variable.class, argumentTerm);
				Constant weight = variable == null ? ZERO : ONE;
				final Product p = cast(Product.class, argumentTerm);
				
				if (p != null) {
					weight = (Constant) p.getOperands().get(0);
					variable = (Variable) p.getOperands().get(1);
				} else if (variable == null) {
					if (!(argumentTerm instanceof Constant)) {
						debugError("Unexpected term:", argumentTerm);
					}
					
					weight = constant(argumentTerm.getAsDouble());
				}
				
				if (!ZERO.equals(weight)) {
					weights.put(variable, weight);
				}
			}
			
			if (!weights.isEmpty()) {
				
				if (hiddenLayer == null) {
					hiddenLayer = result.addLayer(activation);
				}
				
				if (activation == COS) {
					addNeuron(hiddenLayer, weights, input);
					magnitudes.add(magnitude.getAsDouble());
				} else {
					for (int k = 0; k <= cosApproximationQuality; ++k) {
						addNeuron(hiddenLayer, weights, input)[n] -= (2.0 * k - 0.5) * Math.PI;
						magnitudes.add(magnitude.getAsDouble());
						addNeuron(hiddenLayer, weights, input)[n] -= (2.0 * k + 0.5) * Math.PI;
						magnitudes.add(-magnitude.getAsDouble());
					}
					
					bias = approximate(subtract(bias, magnitude), EPSILON);
				}
			}
		}
		
		{
			final double[] outputNeuron = result.addLayer(IDENTITY).addNeuron();
			final int m = magnitudes.size();
			
			for (int i = 0; i < m; ++i) {
				outputNeuron[i] = magnitudes.get(i);
			}
			
			outputNeuron[m] = bias.getAsDouble();
			
			return result;
		}
	}
	
	private static final double[] addNeuron(final Layer layer, final Map<Variable, Constant> weights, final Object[] input) {
		final int n = input.length;
		final double[] result = layer.addNeuron();
		
		for (int i = 0; i < n; ++i) {
			final Constant weight = weights.get(input[i]);
			
			if (weight != null) {
				result[i] = weight.getAsDouble();
			}
		}
		
		{
			final Constant weight = weights.get(null);
			
			if (weight != null) {
				result[n] = weight.getAsDouble();
			}
		}
		
		return result;
	}
	
	public static final double identity(final double x) {
		return x;
	}
	
	public static final double sinmoid(final double x) {
		if (x < -Math.PI / 2.0) {
			return -1.0;
		}
		
		if (x < Math.PI / 2.0) {
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
