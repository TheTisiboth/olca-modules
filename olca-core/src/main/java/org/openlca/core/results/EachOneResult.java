package org.openlca.core.results;

import java.util.Iterator;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.ImpactIndex;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.matrix.format.MatrixReader;
import org.openlca.core.results.providers.SimpleResultProvider;
import org.openlca.julia.Julia;
import org.openlca.julia.JuliaSolver;
import org.openlca.util.Pair;

/**
 * Calculates the (simple) result for each process-product in a given database
 * or a set of matrices.
 */
public class EachOneResult {

	private final IDatabase db;
	private MatrixData data;
	private int next = 0;

	private MatrixReader inverse;
	private double[] diagA;
	private MatrixReader lci;
	private MatrixReader lcia;

	private EachOneResult(IDatabase db) {
		this.db = db;
	}

	public static EachOneResult of(IDatabase db) {
		return new EachOneResult(db);
	}

	public static EachOneResult of(IDatabase db, MatrixData data) {
		var eor = new EachOneResult(db);
		eor.data = data;
		return eor;
	}

	public Iterable<Pair<ProcessProduct, SimpleResult>> get() {
		if (!Julia.isLoaded()) {
			Julia.load();
		}
		if (data == null) {
			var techIndex = TechIndex.of(db);
			data = MatrixData.of(db, techIndex)
				.withImpacts(ImpactIndex.of(db))
				.build();
		}
		var solver = new JuliaSolver();
		inverse = solver.invert(data.techMatrix);
		diagA = data.techMatrix.diag();
		lci = solver.multiply(data.flowMatrix, inverse);
		if (data.impactMatrix != null) {
			lcia = solver.multiply(data.impactMatrix, lci);
		}
		return new Iter();
	}

	private class Iter implements
		Iterator<Pair<ProcessProduct, SimpleResult>>,
		Iterable<Pair<ProcessProduct, SimpleResult>> {

		@Override
		public boolean hasNext() {
			if (data == null || data.techIndex == null)
				return false;
			return next < data.techIndex.size();
		}

		@Override
		public Pair<ProcessProduct, SimpleResult> next() {

			var p = SimpleResultProvider.of(data.techIndex)
				.withFlowIndex(data.flowIndex)
				.withImpactIndex(data.impactIndex);

			if (inverse != null) {
				var scalingVector = inverse.getColumn(next);
				p.withScalingVector(scalingVector);

				if (diagA != null) {
					var totalRequirements = new double[diagA.length];
					for (int i = 0; i < diagA.length; i++) {
						totalRequirements[i] = diagA[i] * scalingVector[i];
					}
					p.withTotalRequirements(totalRequirements);
				}
			}
			if (lci != null) {
				p.withTotalFlows(lci.getColumn(next));
			}
			if (lcia != null) {
				p.withTotalImpacts(lcia.getColumn(next));
			}

			var result = p.toResult();
			var product = data.techIndex.at(next);
			if (product.isWaste()) {
				swapSign(result.scalingVector);
				swapSign(result.totalRequirements);
				swapSign(result.totalFlowResults);
				swapSign(result.totalImpactResults);
			}

			next++;
			return Pair.of(product, result);
		}

		private void swapSign(double[] values) {
			if (values == null)
				return;
			for (int i = 0; i < values.length; i++) {
				var val = values[i];
				if (val != 0) {
					values[i] = -val;
				}
			}
		}

		@Override
		public Iterator<Pair<ProcessProduct, SimpleResult>> iterator() {
			return this;
		}
	}

}
