package jnnet4;

import static java.lang.Double.parseDouble;
import static jnnet4.FeedforwardNeuralNetwork.reserve;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import jnnet.DoubleList;

/**
 * @author codistmonk (creation 2014-03-07)
 */
public final class TrainingData implements Serializable {
	
	private final Map<String, Integer> labelIds;
	
	private final Map<String, AtomicInteger> labelCounts;
	
	private final List<String> labels;
	
	private final DoubleList data;
	
	private int step;
	
	public TrainingData(final String resourcePath) {
		this.labelIds = new LinkedHashMap<String, Integer>();
		this.labelCounts = new LinkedHashMap<String, AtomicInteger>();
		this.labels = new ArrayList<String>();
		this.data = new DoubleList();
		
		final Scanner scanner = new Scanner(getResourceAsStream(resourcePath));
		double[] buffer = {};
		int lineId = 0;
		
		try {
			while (scanner.hasNext()) {
				if (lineId  % 1000 == 0) {
					debugPrint("lineId:", lineId);
				}
				
				final String[] line = scanner.nextLine().trim().split("(\\s|,)+");
				final int n = line.length;
				
				if (2 <= n) {
					buffer = reserve(buffer, n);
					final int labelOffset = n - 1;
					boolean itemIsValid = true;
					
					for (int i = 0; i < labelOffset && itemIsValid; ++i) {
						try {
							buffer[i] = parseDouble(line[i]);
						} catch (final NumberFormatException exception) {
							itemIsValid = false;
						}
					}
					
					if (itemIsValid) {
						this.step = n;
						
						for (int i = 0; i < labelOffset; ++i) {
							this.data.add(buffer[i]);
						}
						
						final String label = line[labelOffset];
						Integer labelId = this.getLabelIds().get(label);
						
						if (labelId == null) {
							labelId = this.getLabels().size();
							this.getLabelIds().put(label, labelId);
							this.getLabels().add(label);
						}
						
						this.data.add(labelId);
						getOrCreate(this.getLabelCounts(), label, ATOMIC_INTEGER_FACTORY).incrementAndGet();
					}
				}
				
				++lineId;
			}
			
			debugPrint("labelCounts:", this.getLabelCounts());
		} finally {
			scanner.close();
		}
	}
	
	public final Map<String, Integer> getLabelIds() {
		return this.labelIds;
	}
	
	public final List<String> getLabels() {
		return this.labels;
	}
	
	public final Map<String, AtomicInteger> getLabelCounts() {
		return this.labelCounts;
	}
	
	public final double[] getData() {
		return this.data.toArray();
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
	
	public static final DefaultFactory<AtomicInteger> ATOMIC_INTEGER_FACTORY = DefaultFactory.forClass(AtomicInteger.class);
	
}
