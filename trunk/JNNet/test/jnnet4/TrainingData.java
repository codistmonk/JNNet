package jnnet4;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author codistmonk (creation 2014-03-07)
 */
public final class TrainingData implements Serializable {
	
	private final double[] data;
	
	private int step;
	
	public TrainingData(final String resourcePath) {
		final List<Double> data = new ArrayList<Double>();
		
		final Scanner scanner = new Scanner(getResourceAsStream(resourcePath));
		
		try {
			while (scanner.hasNext()) {
				final String[] line = scanner.nextLine().trim().split("\\s+");
				final int n = line.length;
				
				if (2 <= n) {
					this.step = n;
					
					for (int i = 0; i < n; ++i) {
						data.add(parseDouble(line[i]));
					}
				}
			}
			
			final int n = data.size();
			this.data = new double[n];
			
			for (int i = 0; i < n; ++i) {
				this.data[i] = data.get(i);
			}
		} finally {
			scanner.close();
		}
	}
	
	public final double[] getData() {
		return this.data;
	}
	
	public final int getStep() {
		return this.step;
	}
	
	public final int getItemCount() {
		return this.getData().length / this.getStep();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 5770717293628383428L;
	
}
