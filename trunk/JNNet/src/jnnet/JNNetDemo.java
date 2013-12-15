package jnnet;

import static java.lang.Math.exp;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jnnet.JNNetDemo.Neuron.Input;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-12-14)
 */
public final class JNNetDemo {
	
	private JNNetDemo() {
		throw new IllegalInstantiationException();
	}
	
	public static final ConstantValueSource ONE = new ConstantValueSource(1.0);
	
	/**
	 * @param commandLineArguments
	 */
	public static final void main(final String[] commandLineArguments) {
		final ModifiableValueSource xSource = new ModifiableValueSource();
		final ModifiableValueSource ySource = new ModifiableValueSource();
		final List<Neuron> network = new ArrayList<Neuron>();
		
		network.add(halfspace(xSource, ySource, -10.0, +10.0, +1.0, -1.0));
		network.add(halfspace(xSource, ySource, +10.0, +10.0, -1.0, -1.0));
		network.add(halfspace(xSource, ySource, + 0.0, -10.0, +0.0, +1.0));
		final int n = network.size();
		network.add(combineLast(network, 1.0 / n, 1.0 / n, 1.0 / n));
		
		final Neuron output = get(network, -1);
		final int w = 512;
		final int h = w;
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		final TicToc timer = new TicToc();
		
		debugPrint(new Date(timer.tic()));
		
		for (int y = 0; y < h; ++y) {
			ySource.setValue(h / 2.0 - y);
			for (int x = 0; x < w; ++x) {
				xSource.setValue(x - w / 2.0);
				
				for (final Neuron neuron : network) {
					neuron.update();
				}
				
				image.setRGB(x, y, 0xFF000000 | (0x00010101 * (int) (output.getValue() * 255.0)));
			}
		}
		
		debugPrint(timer.toc());
		
		SwingTools.show(image, "ANN output", true);
	}
	
	public static final Neuron combineLast(final List<Neuron> network, final double... weights) {
		final int neuronCount = network.size();
		final int lastCount = weights.length;
		final Input[] inputs = new Input[lastCount];
		
		for (int i = 0; i < lastCount; ++i) {
			inputs[i] = new Input(network.get(neuronCount - lastCount + i), weights[i]);
		}
		
		return new Neuron(inputs);
	}
	
	public static final Neuron halfspace(final ValueSource xSource, final ValueSource ySource,
			final double locationX, final double locationY, final double directionX, final double directionY) {
		return new Neuron(
				new Input(ONE, - (locationX * directionX + locationY * directionY)),
				new Input(xSource, directionX),
				new Input(ySource, directionY));
	}
	
	public static final <T> T get(final List<T> list, final int index) {
		return 0 <= index ? list.get(index) : list.get(index + list.size());
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static abstract interface ValueSource extends Serializable {
		
		public abstract void update();
		
		public abstract double getValue();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class ConstantValueSource implements ValueSource {
		
		private final double value;
		
		public ConstantValueSource(final double value) {
			this.value = value;
		}
		
		@Override
		public final void update() {
			// NOP
		}
		
		@Override
		public final double getValue() {
			return this.value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 5846379572555159967L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class ModifiableValueSource implements ValueSource {
		
		private double value;
		
		@Override
		public final void update() {
			// NOP
		}
		
		@Override
		public final double getValue() {
			return this.value;
		}
		
		public final void setValue(final double value) {
			this.value = value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -4620991329089575653L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-12-14)
	 */
	public static final class Neuron implements ValueSource {
		
		private final Input[] inputs;
		
		private double value;
		
		public Neuron(final Input... inputs) {
			this.inputs = inputs;
		}
		
		@Override
		public final void update() {
			this.value = 0.0;
			
			for (final Input input : this.inputs) {
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
			
			private final double weight;
			
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
	
}
