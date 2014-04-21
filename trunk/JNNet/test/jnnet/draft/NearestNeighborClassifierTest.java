package jnnet.draft;

import static jnnet.draft.SimplifiedNeuralBinaryClassifierTest.show;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-11)
 */
public final class NearestNeighborClassifierTest {
	
	@Test
	public final void test() {
		final boolean showClassifier = true;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
//		final Dataset trainingData = new Dataset("jnnet/2spirals.txt");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/gisette/gisette_train.data");
//		final Dataset trainingData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 0, 500000);
		final CSVDataset trainingData = new CSVDataset("../Libraries/datasets/SUSY.csv", 0, 0, 500000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
//		debugPrint("Loading validation dataset started", new Date(timer.tic()));
//		final Dataset validationData = new Dataset("../Libraries/datasets/gisette/gisette_valid.data");
//		debugPrint("Loading validation dataset done in", timer.toc(), "ms");
		
//		debugPrint("Loading test dataset started", new Date(timer.tic()));
//		final Dataset testData = new Dataset("../Libraries/datasets/HIGGS.csv", 0, 11000000-500000, 500000);
		final CSVDataset testData = new CSVDataset("../Libraries/datasets/SUSY.csv", 0, 5000000-500000, 500000);
//		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		debugPrint("Building classifier started", new Date(timer.tic()));
		final NearestNeighborClassifier classifier = new NearestNeighborClassifier(trainingData);
		debugPrint("Building classifier done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
		final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData, null);
		debugPrint("training:", confusionMatrix);
		debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
		
//		debugPrint("Evaluating classifier on validation set started", new Date(timer.tic()));
//		debugPrint("test:", classifier.evaluate(validationData));
//		debugPrint("Evaluating classifier on validation set done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
		debugPrint("test:", classifier.evaluate(testData, null));
		debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
		
		if (showClassifier && classifier.getInputDimension() == 2) {
			show(classifier, 256, 16.0, trainingData.getData_());
		}
		
		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
}

/**
 * @author codistmonk (creation 2014-03-11)
 */
final class NearestNeighborClassifier implements BinaryClassifier {
	
	private final double[] prototypes;
	
	private final int inputDimension;
	
	public NearestNeighborClassifier(final CSVDataset trainingData) {
		this.prototypes = trainingData.getData_();
		this.inputDimension= trainingData.getItemSize() - 1;
	}
	
	@Override
	public final int getInputDimension() {
		return this.inputDimension;
	}
	
	@Override
	public final boolean accept(final double... item) {
		final int n = this.prototypes.length;
		final int step = this.getInputDimension() + 1;
		double nearestDistance = Double.POSITIVE_INFINITY;
		boolean result = false;
		
		for (int i = 0; i < n; i += step) {
			final double d = squaredDistance(item, this.prototypes, i);
			
			if (d < nearestDistance) {
				nearestDistance = d;
				result = this.prototypes[i + step - 1] == 1.0;
			}
		}
		
		return result;
	}
	
	@Override
	public final SimpleConfusionMatrix evaluate(final Dataset dataset, final EvaluationMonitor monitor) {
		return Default.defaultEvaluate(this, dataset, monitor);
	}
	
	public static final double squaredDistance(final double[] v1, final double[] data, final int offset) {
		double result = 0.0;
		final int n = v1.length;
		
		for (int i = 0; i < n; ++i) {
			result += square(v1[i] - data[offset + i]);
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 3974020901035970415L;
	
}
