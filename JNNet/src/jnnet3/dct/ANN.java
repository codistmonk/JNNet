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

import net.sourceforge.aprog.tools.MathTools.Statistics;

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
	
	private static final boolean DEBUG = false;
	
	static final double EPSILON = 1.0E-12;
	
	static final DoubleUnaryOperator COS = Math::cos;
	
	static final DoubleUnaryOperator SINMOID = ANN::sinmoid;
	
	static final DoubleUnaryOperator SIGMOID = ANN::sigmoid;
	
	static final DoubleUnaryOperator IDENTITY = ANN::identity;
	
	public static final ANN newIDCTNetwork(final Object dct) {
		return newIDCTNetwork(dct, 1.0, 0.0, SIGMOID);
	}
	
	public static final ANN newIDCTNetwork(final Object dct, final double inputScale, final double inputBias) {
		return newIDCTNetwork(dct, inputScale, inputBias, SIGMOID);
	}
	
	public static final ANN newIDCTNetwork(final Object dct, final DoubleUnaryOperator activation) {
		return newIDCTNetwork(dct, 1.0, 0.0, activation);
	}
	
	public static final ANN newIDCTNetwork(final Object dct, final double inputScale, final double inputBias, final DoubleUnaryOperator activation) {
		final int[] dimensions = DCT.getDimensions(dct);
		final int n = dimensions.length;
		final Object[] input = new Expression[n];
		
		for (int i = 0; i < n; ++i) {
			input[i] = variable("x" + i);
		}
		
		final ANN result = new ANN(n);
		final Expression idct = separateCosProducts(idct(dct, input), EPSILON);
		
		if (DEBUG) {
			debugPrint(idct);
		}
		
		double bias = 0.0;
		final List<Expression> terms = idct instanceof Sum ? ((Sum) idct).getOperands() : Arrays.asList(idct);
		final Map<Variable, Double> weights = new HashMap<>();
		Layer hiddenLayer = null;
		final List<Double> magnitudes = new ArrayList<>();
		
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
				
				bias += term.getAsDouble();
				
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
					weights.put(variable, weight.getAsDouble());
				}
			}
			
			if (!weights.isEmpty()) {
				if (hiddenLayer == null) {
					hiddenLayer = result.addLayer(activation);
				}
				
				if (activation == COS) {
					addNeuron(hiddenLayer, weights, inputScale, inputBias, 1.0, input);
					magnitudes.add(magnitude.getAsDouble());
				} else {
					final Statistics statistics = new Statistics();
					final double phase = weights.getOrDefault(null, 0.0);
					final double h = activation == SIGMOID ? 2.5 : 1.0;
					
					statistics.addValue(phase);
					
					for (int i = 0; i < n; ++i) {
						// assume 0 <= input[i] <= dimensions[i] - 1
						final double value = (dimensions[i] - 1) * weights.getOrDefault(input[i], 0.0);
						statistics.addValue(statistics.getMinimum() + value);
						statistics.addValue(statistics.getMaximum() + value);
					}
					
					final int firstK = (int) Math.floor(statistics.getMinimum() / 2.0 / Math.PI);
					final int lastK = (int) Math.floor(statistics.getMaximum() / 2.0 / Math.PI);
					final double z = firstK * 2.0 * Math.PI;
					final double c0 = protocosinoid(activation, firstK, lastK, z, h);
					final double cpi = protocosinoid(activation, firstK, lastK, z + Math.PI, h);
					final double scale = 2.0 / (c0 - cpi);
					
					for (int k = firstK; k <= lastK; ++k) {
						addNeuron(hiddenLayer, weights, inputScale, inputBias, h, input)[n] -= (2.0 * k + 0.5) * Math.PI * h;
						magnitudes.add(magnitude.getAsDouble() * scale);
						addNeuron(hiddenLayer, weights, inputScale, inputBias, h, input)[n] -= (2.0 * k + 1.5) * Math.PI * h;
						magnitudes.add(-magnitude.getAsDouble() * scale);
					}
					
					bias += magnitude.getAsDouble() * (1.0 - scale * c0);
				}
			}
		}
		
		{
			final double[] outputNeuron = result.addLayer(IDENTITY).addNeuron();
			final int m = magnitudes.size();
			
			for (int i = 0; i < m; ++i) {
				outputNeuron[i] = magnitudes.get(i);
			}
			
			outputNeuron[m] = bias;
			
			return result;
		}
	}
	
	private static final double pair(final DoubleUnaryOperator activation, final int k, final double x, final double h) {
		return activation.applyAsDouble(x * h - (2.0 * k + 0.5) * Math.PI * h)
				- activation.applyAsDouble(x * h - (2.0 * k + 1.5) * Math.PI * h);
	}
	
	private static final double protocosinoid(final DoubleUnaryOperator activation, final int firstK, final int lastK, final double x, final double h) {
		double result = 0.0;
		
		for (int k = firstK; k <= lastK; ++k) {
			result += pair(activation, k, x, h);
		}
		
		return result;
	}
	
	private static final double[] addNeuron(final Layer layer, final Map<Variable, Double> weights,
			final double inputScale, final double inputBias, final double h, final Object[] input) {
		final int n = input.length;
		final double[] result = layer.addNeuron();
		double w = 0.0;
		
		for (int i = 0; i < n; ++i) {
			final Double weight = weights.get(input[i]);
			
			if (weight != null) {
				w += weight;
				result[i] = weight * inputScale * h;
			}
		}
		
		{
			final Double weight = weights.get(null);
			
			if (weight != null) {
				result[n] = (weight + w * inputBias) * h;
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
	
	public static final double sigmoid(final double x) {
		return 1.0 / (1.0 + Math.exp(-x));
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
