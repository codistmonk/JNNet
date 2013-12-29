package jnnet4;

import static jnnet4.JNNetTools.outputs;
import static jnnet4.JNNetTools.sigmoid;
import static org.junit.Assert.*;
import jnnet4.ArtificialNeuralNetwork.BiasSourceIndex;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-12-26)
 */
public final class ArtificialNeuralNetworkTest {
	
	@Test
	public final void test1() {
		final ArtificialNeuralNetwork network = new ArtificialNeuralNetwork(2, BiasSourceIndex.NONE);
		
		assertEquals(2, network.getInputCount());
		assertEquals(2, network.getVariableInputCount());
		assertEquals(-1, network.getBiasSourceIndex());
		assertEquals(1, network.getLayerCount());
		assertEquals(2, network.getNeuronCount());
		assertEquals(0, network.getNeuronWeightCount(0));
		assertEquals(0, network.getNeuronWeightCount(1));
		
		network.setInputs(1.0, 3.0);
		
		assertEquals(1.0, network.getNeuronValue(0), 0.0);
		assertEquals(3.0, network.getNeuronValue(1), 0.0);
		
		network.updateAllLayers();
		
		assertArrayEquals(outputs(1.0, 3.0), network.getOutputLayerValues(), 0.0);
	}
	
	@Test
	public final void test2() {
		final ArtificialNeuralNetwork network = new ArtificialNeuralNetwork(2, BiasSourceIndex.NONE);
		
		network.addLayer();
		
		assertEquals(2, network.getLayerCount());
		
		network.addNeuron(1.0, -1.0);
		
		assertEquals(3, network.getNeuronCount());
		assertEquals(2, network.getLayerNeuronCount(0));
		assertEquals(1, network.getLayerNeuronCount(1));
		assertEquals(2, network.getNeuronWeightCount(2));
		assertEquals(1.0, network.getWeight(2, 0), 0.0);
		assertEquals(-1.0, network.getWeight(2, 1), 0.0);
		assertEquals(0.0, network.getBias(2), 0.0);
		
		network.setInputs(1.0, 3.0).updateAllLayers();
		
		assertEquals(sigmoid(-2.0), network.getNeuronValue(2), 0.0);
		assertArrayEquals(outputs(sigmoid(-2.0)), network.getOutputLayerValues(), 0.0);
	}
	
	@Test
	public final void test3() {
		final ArtificialNeuralNetwork network = new ArtificialNeuralNetwork(3, BiasSourceIndex.FIRST);
		
		assertEquals(3, network.getInputCount());
		assertEquals(2, network.getVariableInputCount());
		assertEquals(0, network.getBiasSourceIndex());
		
		network.addLayer();
		network.addNeuron(2.0, 1.0, -1.0);
		
		assertEquals(4, network.getNeuronCount());
		assertEquals(3, network.getLayerNeuronCount(0));
		assertEquals(1, network.getLayerNeuronCount(1));
		assertEquals(3, network.getNeuronWeightCount(3));
		assertEquals(2.0, network.getWeight(3, 0), 0.0);
		assertEquals(1.0, network.getWeight(3, 1), 0.0);
		assertEquals(-1.0, network.getWeight(3, 2), 0.0);
		assertEquals(2.0, network.getBias(3), 0.0);
		
		network.setInputs(1.0, 3.0).updateAllLayers();
		
		assertArrayEquals(outputs(sigmoid(0.0)), network.getOutputLayerValues(), 0.0);
	}
	
	@Test
	public final void test4() {
		final ArtificialNeuralNetwork network = new ArtificialNeuralNetwork(3, BiasSourceIndex.LAST);
		
		assertEquals(3, network.getInputCount());
		assertEquals(2, network.getVariableInputCount());
		assertEquals(2, network.getBiasSourceIndex());
		
		network.addLayer();
		network.addNeuron(1.0, -1.0, 2.0);
		
		assertEquals(4, network.getNeuronCount());
		assertEquals(3, network.getLayerNeuronCount(0));
		assertEquals(1, network.getLayerNeuronCount(1));
		assertEquals(3, network.getNeuronWeightCount(3));
		assertEquals(1.0, network.getWeight(3, 0), 0.0);
		assertEquals(-1.0, network.getWeight(3, 1), 0.0);
		assertEquals(2.0, network.getWeight(3, 2), 0.0);
		assertEquals(2.0, network.getBias(3), 0.0);
		
		network.setInputs(1.0, 3.0).updateAllLayers();
		
		assertArrayEquals(outputs(sigmoid(0.0)), network.getOutputLayerValues(), 0.0);
	}
	
}
