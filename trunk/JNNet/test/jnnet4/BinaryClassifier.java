package jnnet4;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-03-10)
 */
public abstract interface BinaryClassifier extends Serializable {
	
	public abstract int getStep();
	
	public abstract boolean accept(double... item);
	
	public abstract SimpleConfusionMatrix evaluate(Dataset trainingData);
	
	/**
	 * {@value}.
	 */
	public static final long LOGGING_MILLISECONDS = 5000L;
	
}
