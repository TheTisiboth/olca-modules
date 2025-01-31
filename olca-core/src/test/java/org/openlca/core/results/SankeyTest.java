package org.openlca.core.results;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.openlca.core.Tests;
import org.openlca.core.math.LcaCalculator;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.matrix.format.JavaMatrix;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class SankeyTest {

	@Test
	public void testCycles() {
		var data = new MatrixData();
		data.techIndex = new TechIndex(product(1));
		data.techIndex.setDemand(1.0);
		data.techIndex.add(product(2));
		data.techIndex.add(product(3));
		data.techMatrix = JavaMatrix.of(new double[][]{
				{1.0, 0.0, 0.0},
				{-1.0, 1.0, -0.1},
				{0.0, -2.0, 1.0},
		});

		data.flowIndex = FlowIndex.create();
		var flow = new FlowDescriptor();
		flow.id = 42;
		data.flowIndex.add(IndexFlow.outputOf(flow));
		data.flowMatrix = JavaMatrix.of(new double[][]{
				{1.0, 2.0, 3.0},
		});

		var calculator = new LcaCalculator(Tests.getDb(), data);
		var result = calculator.calculateFull();

		var sankey = Sankey.of(data.flowIndex.at(0), result)
				.build();
		Assert.assertEquals(3, sankey.nodeCount);
		var visited = new AtomicInteger(0);
		sankey.traverse(node -> {
			visited.incrementAndGet();

			switch (node.index) {
				case 0:
					Assert.assertEquals(1.0, node.direct, 1e-10);
					Assert.assertEquals(11.0, node.total, 1e-10);
					Assert.assertEquals(1.0, node.share, 1e-10);
					break;

				case 1:
					Assert.assertEquals(2.5, node.direct, 1e-10);
					Assert.assertEquals(10, node.total, 1e-10);
					Assert.assertEquals(10.0 / 11.0, node.share, 1e-10);
					break;

				case 2:
					Assert.assertEquals(7.5, node.direct, 1e-10);
					Assert.assertEquals(8.0, node.total, 1e-10);
					Assert.assertEquals(8.0 / 11.0, node.share, 1e-10);
					break;
			}
		});
		Assert.assertEquals(3, visited.get());
	}

	private ProcessProduct product(int i) {
		var process = new ProcessDescriptor();
		process.id = i;
		process.name = "process " + i;
		var flow = new FlowDescriptor();
		flow.id = i;
		flow.name = "product " + i;
		return ProcessProduct.of(process, flow);
	}

}
