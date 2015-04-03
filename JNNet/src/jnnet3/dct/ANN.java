package jnnet3.dct;

import static java.util.Arrays.fill;
import static java.util.stream.Collectors.toCollection;
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

import jnnet3.dct.MiniCAS.Constant;
import jnnet3.dct.MiniCAS.Cos;
import jnnet3.dct.MiniCAS.Expression;
import jnnet3.dct.MiniCAS.NaryOperation;
import jnnet3.dct.MiniCAS.Product;
import jnnet3.dct.MiniCAS.Sum;

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
	
	static final double EPSILON = 1.0E-8;
	
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
		final Expression idct = approximate(separateCosProducts(idct(dct, input)), EPSILON).accept(new AddPhasors(EPSILON));
		
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
					for (int k = -cosApproximationQuality; k <= cosApproximationQuality; ++k) {
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
	
	/**
	 * @author codistmonk (creation 2015-04-02)
	 */
	public static final class AddPhasors implements Expression.Rewriter {
		
		private final double epsilon;
		
		public AddPhasors(final double epsilon) {
			this.epsilon = epsilon;
		}
		
		@Override
		public final Expression visit(final NaryOperation operation) {
			final List<Expression> operands = operation.getOperands().stream().map(this).collect(toCollection(ArrayList::new));
			final Sum sum = cast(Sum.class, operation);
			
			if (sum != null) {
				try {
					final List<CosTerm> cosTerms = operands.stream().map(CosTerm::new).collect(toCollection(ArrayList::new));
					
					operands.clear();
					
					for (int i = 0; i < cosTerms.size(); ++i) {
						final CosTerm cosTermI = cosTerms.get(i);
						final Expression approximatedFrequencyI = approximate(cosTermI.getFrequency(), this.epsilon);
						final List<Double> magnitudes = new ArrayList<>();
						final List<Double> phases = new ArrayList<>();
						
						magnitudes.add(cosTermI.getMagnitude().getAsDouble());
						phases.add(cosTermI.getPhase().getAsDouble());
						
						for (int j = i + 1; j < cosTerms.size(); ++j) {
							final CosTerm cosTermJ = cosTerms.get(j);
							final Expression approximatedFrequencyJ = approximate(cosTermJ.getFrequency(), this.epsilon);
							final Expression approximatedOppositeFrequencyJ = approximate(multiply(MINUS_ONE, cosTermJ.getFrequency()), this.epsilon);
							
							if (approximatedFrequencyI.equals(approximatedFrequencyJ, this.epsilon)) {
								phases.add(cosTermJ.getPhase().getAsDouble());
							} else if (approximatedFrequencyI.equals(approximatedOppositeFrequencyJ, this.epsilon)) {
								phases.add(-cosTermJ.getPhase().getAsDouble());
							} else {
								continue;
							}
							
							magnitudes.add(cosTermJ.getMagnitude().getAsDouble());
							cosTerms.remove(j);
						}
						
						final int n = magnitudes.size();
						
						if (n == 1) {
							operands.add(cosTermI.getTerm());
						} else {
							double a2 = 0.0;
							double tandN = 0.0;
							double tandD = 0.0;
							
							for (int j = 0; j < n; ++j) {
								final double mj = magnitudes.get(j);
								final double pj = phases.get(j);
								
								for (int k = 0; k < n; ++k) {
									final double mk = magnitudes.get(k);
									final double pk = phases.get(k);
									
									a2 += mj * mk * Math.cos(pj - pk);
								}
								
								tandN += mj * Math.sin(pj);
								tandD += mj * Math.cos(pj);
							}
							
							final double magnitude = Math.sqrt(a2);
							final double phase = Math.atan2(tandN, tandD);
							
							operands.add(multiply(magnitude, cos(add(phase, cosTermI.getFrequency()))));
						}
					}
				} catch (final Exception exception) {
					ignore(exception);
				}
			}
			
			return approximate(operation.maybeNew(operands), this.epsilon);
		}
		
		private static final long serialVersionUID = -2969190749620211785L;
		
		/**
		 * @author codistmonk (creation 2015-04-02)
		 */
		public static final class CosTerm implements Serializable {
			
			private final Expression term;
			
			private final Constant magnitude;
			
			private final Cos cos;
			
			private final Constant phase;
			
			private final Expression frequency;
			
			public CosTerm(final Expression term) {
				this.term = term;
				
				{
					final Product product = cast(Product.class, term);
					
					if (product != null) {
						if (product.getOperands().size() != 2) {
							throw new IllegalArgumentException(term.toString());
						}
						
						this.magnitude = (Constant) product.getOperands().get(0);
						this.cos = (Cos) product.getOperands().get(1);
					} else {
						final Cos cos = cast(Cos.class, term);
						
						if (cos != null) {
							this.magnitude = ONE;
							this.cos = cos;
						} else {
							this.magnitude = (Constant) term;
							this.cos = cos(ZERO);
						}
					}
				}
				
				{
					final Expression argument = this.cos.getOperand();
					final Sum sum = cast(Sum.class, argument);
					
					if (sum != null) {
						final List<Expression> sumOperands = sum.getOperands();
						final Constant phase = cast(Constant.class, sumOperands.get(0));
						
						if (phase != null) {
							this.phase = phase;
							this.frequency = add(sumOperands.subList(1, sumOperands.size()).toArray());
						} else {
							this.phase = ZERO;
							this.frequency = argument;
						}
					} else {
						final Constant phase = cast(Constant.class, argument);
						
						if (phase != null) {
							this.phase = phase;
							this.frequency = ZERO;
						} else {
							this.phase = ZERO;
							this.frequency = argument;
						}
					}
				}
			}
			
			public final Expression getTerm() {
				return this.term;
			}
			
			public final Constant getMagnitude() {
				return this.magnitude;
			}
			
			public final Cos getCos() {
				return this.cos;
			}
			
			public final Constant getPhase() {
				return this.phase;
			}
			
			public final Expression getFrequency() {
				return this.frequency;
			}
			
			@Override
			public final String toString() {
				return "(" + this.getMagnitude() + " " + this.getPhase() + " " + this.getFrequency() + ")";
			}
			
			private static final long serialVersionUID = -4928748997284860125L;
			
		}
		
	}
	
}
