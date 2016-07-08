package weka.classifiers.functions;

import static jnnet.JNNetTools.doubles;
import static multij.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.io.FileReader;

import multij.tools.Tools;

import org.junit.Test;

import weka.classifiers.functions.JNNetBinaryClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author codistmonk (creation 2014-04-20)
 */
public final class JNNetBinaryClassifierTest {
	
	@Test
	public final void testBuildClassifierInstances() throws Exception {
		final JNNetBinaryClassifier plugin = new JNNetBinaryClassifier();
		final Instances data = newWekaDataset();
		
		plugin.buildClassifier(data);
	}
	
	@Test
	public final void testToString() throws Exception {
		final JNNetBinaryClassifier plugin = new JNNetBinaryClassifier();
		final Instances data = newWekaDataset();
		
		plugin.buildClassifier(data);
		
		assertEquals("Feedforward Neural network: 1 -> 1 (step) -> 1 (step) -> 1 (step)"
				, plugin.toString());
	}
	
	@Test
	public final void testClassifyInstanceInstance1() throws Exception {
		final JNNetBinaryClassifier plugin = new JNNetBinaryClassifier();
		final Instances data = newWekaDataset();
		
		plugin.buildClassifier(data);
		
		for (int i = 0; i < data.numInstances(); ++i) {
			final Instance instance = data.instance(i);
			
			assertEquals(instance.value(data.classIndex()), plugin.classifyInstance(instance), 0.0);
		}
	}
	
	@Test
	public final void testClassifyInstanceInstance2() throws Exception {
		final JNNetBinaryClassifier plugin = new JNNetBinaryClassifier();
		final Instances data = new Instances(new FileReader("test/weka/classifiers/functions/breast-cancer.arff"));
		
		data.setClassIndex(data.numAttributes() - 1);
		
		debugPrint(data.numAttributes(), data.numClasses(), data.classIndex(), data.numInstances());
		
		plugin.buildClassifier(data);
		
		debugPrint(plugin.evaluate(data));
	}
	
	public static final Instances newWekaDataset() {
		final FastVector attributes = new FastVector(2);
		
		attributes.addElement(new Attribute("x"));
		attributes.addElement(new Attribute("y"));
		
		final Instances result = new Instances(Tools.getThisMethodName(), attributes, 0);
		
		result.setClassIndex(1);
		
		result.add(new DenseInstance(1.0, doubles(1.0, 0.0)));
		result.add(new DenseInstance(1.0, doubles(2.0, 0.0)));
		result.add(new DenseInstance(1.0, doubles(3.0, 1.0)));
		result.add(new DenseInstance(1.0, doubles(4.0, 1.0)));
		
		return result;
	}
	
}