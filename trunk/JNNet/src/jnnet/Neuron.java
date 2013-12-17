package jnnet;

import static java.lang.Math.exp;

/**
 * @author codistmonk (creation 2013-12-14)
 */
public final class Neuron implements ValueSource {
	
	private final Input[] inputs;
	
	private double value;
	
	public Neuron(final Input... inputs) {
		this.inputs = inputs;
	}
	
	public final Input[] getInputs() {
		return this.inputs;
	}
	
	@Override
	public final void update() {
		this.value = 0.0;
		
		for (final Input input : this.getInputs()) {
			this.value += input.getValue();
		}
		
		this.value = sigmoid(this.value);
	}
	
	@Override
	public final double getValue() {
		return this.value;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 966129720042922563L;
	
	public static final double sigmoid(final double x) {
		return 1.0 / (1.0 + exp(-x));
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class Input implements ValueSource {
		
		private final ValueSource valueSource;
		
		private double weight;
		
		public Input(final ValueSource valueSource, final double weight) {
			this.valueSource = valueSource;
			this.weight = weight;
		}
		
		public final ValueSource getValueSource() {
			return this.valueSource;
		}
		
		public final double getWeight() {
			return this.weight;
		}
		
		public final void setWeight(final double weight) {
			this.weight = weight;
		}
		
		@Override
		public final void update() {
			this.getValueSource().update();
		}
		
		@Override
		public final double getValue() {
			return this.getValueSource().getValue() * this.getWeight();
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6771686408291762051L;
		
	}
	
}
