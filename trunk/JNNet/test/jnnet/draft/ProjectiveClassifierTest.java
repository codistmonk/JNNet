package jnnet.draft;

import static jnnet.draft.ProjectiveClassifier.preview;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import jnnet.BinaryClassifier;
import jnnet.CSVDataset;
import jnnet.SimpleConfusionMatrix;
import jnnet.SimplifiedNeuralBinaryClassifierTest;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-03-11)
 */
public final class ProjectiveClassifierTest {
	
	@Test
	public final void test() {
		final boolean showClassifier = true;
		final boolean previewTrainingData = false;
		final boolean previewTestData = false;
		final int thumbnailSize = 8;
		final TicToc timer = new TicToc();
		
		debugPrint("Loading training dataset started", new Date(timer.tic()));
//		final Dataset trainingData = new Dataset("jnnet/2spirals.txt");
		final CSVDataset trainingData = new CSVDataset("../Libraries/datasets/HIGGS.csv", 0, 0, 500000);
//		final Dataset trainingData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 0, 500000);
		debugPrint("Loading training dataset done in", timer.toc(), "ms");
		
		if (previewTrainingData) {
			final BufferedImage preview = preview(trainingData, thumbnailSize);
			
			try {
				ImageIO.write(preview, "png", new File("higgs_0_500000.png"));
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
			
			SwingTools.show(preview, "Training data", false);
		}
		
		debugPrint("Building classifier started", new Date(timer.tic()));
		final BinaryClassifier classifier = new ProjectiveClassifier(trainingData, thumbnailSize);
		debugPrint("Building classifier done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on training set started", new Date(timer.tic()));
		final SimpleConfusionMatrix confusionMatrix = classifier.evaluate(trainingData, null);
		debugPrint("training:", confusionMatrix);
		debugPrint("Evaluating classifier on training set done in", timer.toc(), "ms");
		
		debugPrint("Loading test dataset started", new Date(timer.tic()));
		final CSVDataset testData = new CSVDataset("../Libraries/datasets/HIGGS.csv", 0, 11000000-500000, 500000);
//		final Dataset testData = new Dataset("../Libraries/datasets/SUSY.csv", 0, 5000000-500000, 500000);
		debugPrint("Loading test dataset done in", timer.toc(), "ms");
		
		debugPrint("Evaluating classifier on test set started", new Date(timer.tic()));
		debugPrint("test:", classifier.evaluate(testData, null));
		debugPrint("Evaluating classifier on test set done in", timer.toc(), "ms");
		
		if (previewTestData) {
			SwingTools.show(preview(testData, thumbnailSize), "Test data", true);
		}
		
		if (showClassifier && classifier.getInputDimension() == 2) {
			SimplifiedNeuralBinaryClassifierTest.show(classifier, 256, 16.0, trainingData.getData());
		}
		
//		assertEquals(0, confusionMatrix.getTotalErrorCount());
	}
	
}
