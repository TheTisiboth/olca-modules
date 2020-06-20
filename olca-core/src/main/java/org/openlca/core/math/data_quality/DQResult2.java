package org.openlca.core.math.data_quality;

import java.math.RoundingMode;
import java.util.List;
import java.util.function.Supplier;

import gnu.trove.map.hash.TLongObjectHashMap;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.results.ContributionResult;

/**
 * Contains the raw data quality data of a setup and result in an efficient
 * data structure.
 */
public class DQResult2 {

	private final DQCalculationSetup setup;
	private final ContributionResult result;

	/**
	 * We store the process data in a byte matrix where the data quality
	 * indicators are mapped to the rows and the process products to the
	 * columns.
	 */
	private byte[][] processData;

	/**
	 * For the exchange data we store a flow*product matrix for each
	 * indicator that holds the respective data quality scores of that
	 * indicator.
	 */
	private BMatrix[] exchangeData;

	/**
	 * A k*m matrix that holds the aggregated flow results for the k
	 * indicators and m flows of the setup. It is calculated by
	 * aggregating the exchange data with the direct flow contribution
	 * result.
	 */
	private BMatrix flowResult;

	static DQResult2 of(IDatabase db, DQCalculationSetup setup,
						ContributionResult result) {
		var r = new DQResult2(setup, result);
		r.loadProcessData(db);
		r.loadExchangeData(db);
		r.calculateFlowResults();
		return r;
	}

	private DQResult2(DQCalculationSetup setup, ContributionResult result) {
		this.setup = setup;
		this.result = result;
	}

	/**
	 * Get the process data quality entry for the given product.
	 */
	public int[] get(ProcessProduct product) {
		if (processData == null)
			return null;
		int col = result.techIndex.getIndex(product);
		if (col < 0)
			return null;
		int[] values = new int[processData.length];
		for (int row = 0; row < processData.length; row++) {
			values[row] = processData[row][col];
		}
		return values;
	}

	/**
	 * Get the exchange data quality entry for the given product and flow.
	 */
	public int[] get(ProcessProduct product, IndexFlow flow) {
		if (exchangeData == null)
			return null;
		int row = result.flowIndex.of(flow);
		int col = result.techIndex.getIndex(product);
		if (row < 0 || col < 0)
			return null;
		int[] values = new int[exchangeData.length];
		for (int k = 0; k < exchangeData.length; k++) {
			values[k] = exchangeData[k].get(row, col);
		}
		return values;
	}

	/**
	 * Get the aggregated result for the given flow.
	 */
	public int[] get(IndexFlow flow) {
		if (flowResult == null)
			return null;
		int col = result.flowIndex.of(flow);
		if (col < 0)
			return null;
		int[] values = new int[flowResult.rows];
		for (int row = 0; row < flowResult.rows; row++) {
			values[row] = flowResult.get(row, col);
		}
		return values;
	}

	private void loadProcessData(IDatabase db) {
		var system = setup.processSystem;
		if (system == null)
			return;

		var n = system.indicators.size();
		processData = new byte[n][];
		for (int i = 0; i < n; i++) {
			processData[i] = new byte[result.techIndex.size()];
		}

		// query the process table
		var techIndex = result.techIndex;
		var sql = "select id, f_dq_system, dq_entry " +
				"from tbl_processes";
		NativeSql.on(db).query(sql, r -> {

			// check that we have a valid entry
			long systemID = r.getLong(2);
			if (systemID != system.id)
				return true;
			var dqEntry = r.getString(3);
			if (dqEntry == null)
				return true;
			var providers = techIndex.getProviders(r.getLong(1));
			if (providers.isEmpty())
				return true;

			// store the values of the entry
			int[] values = system.toValues(dqEntry);
			int _n = Math.min(n, values.length);
			for (int i = 0; i < _n; i++) {
				byte[] data = processData[i];
				byte value = (byte) values[i];
				for (var provider : providers) {
					int col = techIndex.getIndex(provider);
					data[col] = value;
				}
			}
			return true;
		});
	}

	private void loadExchangeData(IDatabase db) {
		var system = setup.exchangeSystem;
		if (system == null || result.flowIndex == null)
			return;

		// allocate a BMatrix for each indicator
		var n = system.indicators.size();
		exchangeData = new BMatrix[n];
		var techIndex = result.techIndex;
		var flowIndex = result.flowIndex;
		for (int i = 0; i < n; i++) {
			exchangeData[i] = new BMatrix(
					flowIndex.size(), techIndex.size());
		}

		// collect the processes (providers) of the result with a
		// matching data quality system
		var providers = new TLongObjectHashMap<List<ProcessProduct>>();
		var sql = "select id, f_exchange_dq_system from tbl_processes";
		NativeSql.on(db).query(sql, r -> {
			long sysID = r.getLong(2);
			if (sysID != system.id)
				return true;
			var processID = r.getLong(1);
			var products = result.techIndex.getProviders(processID);
			if (products.isEmpty())
				return true;
			providers.put(processID, products);
			return true;
		});

		// now, scan the exchanges table and collect all
		// matching data quality entries
		sql = "select f_owner, f_flow, f_location, dq_entry from tbl_exchanges";
		NativeSql.on(db).query(sql, r -> {

			// check that we have a valid entry
			var products = providers.get(r.getLong(1));
			if (products == null)
				return true;
			long flowID = r.getLong(2);
			long locationID = r.getLong(3);
			var dqEntry = r.getString(4);
			if (dqEntry == null)
				return true;
			int row = flowIndex.of(flowID, locationID);
			if (row < 0)
				return true;

			// store the values
			int[] values = system.toValues(dqEntry);
			int _n = Math.min(n, values.length);
			for (int i = 0; i < _n; i++) {
				var data = exchangeData[i];
				byte value = (byte) values[i];
				for (var product : products) {
					int col = techIndex.getIndex(product);
					data.set(row, col, value);
				}
			}
			return true;
		});
	}

	/**
	 * Aggregate the raw exchange DQ values with the direct flow contribution
	 * results if applicable.
	 */
	private void calculateFlowResults() {
		if (setup.aggregationType == AggregationType.NONE
				|| exchangeData == null)
			return;
		var matrixG = result.directFlowResults;
		if (matrixG == null)
			return;

		var system = setup.exchangeSystem;
		int k = system.indicators.size();
		int m = result.flowIndex.size();
		int n = result.techIndex.size();
		flowResult = new BMatrix(k, m);
		int max = system.getScoreCount();

		for (int indicator = 0; indicator < k; indicator++) {
			var b = exchangeData[indicator];
			for (int flow = 0; flow < m; flow++) {

				// collect the n DQ values of the flow
				int[] dqs = new int[n];
				for (int j = 0; j < n; j++) {
					int val = b.get(flow, j);
					if (val == 0 && setup.naHandling == NAHandling.USE_MAX) {
						val = max;
					}
					dqs[j] = val;
				}

				// set the aggregated indicator result
				int flowIdx = flow; // because we need a final var for the closure
				Supplier<double[]> weights = () -> matrixG.getRow(flowIdx);
				flowResult.set(indicator, flow, aggregate(dqs, max, weights));
			}
		}

	}

	private int aggregate(int[] dqs, int max, Supplier<double[]> weightsFn) {
		if (setup.aggregationType == null
				|| setup.aggregationType == AggregationType.NONE)
			return 0;

		if (setup.aggregationType == AggregationType.MAXIMUM) {
			int m = 0;
			for (int dq : dqs) {
				m = Math.max(m, dq);
			}
			return Math.min(m, max);
		}

		boolean square = setup.aggregationType
				== AggregationType.WEIGHTED_SQUARED_AVERAGE;
		double[] weights = weightsFn.get();
		double totalWeight = 0;
		double value = 0;

		for (int i = 0; i < dqs.length; i++) {
			double weight = square
					? Math.pow(weights[i], 2)
					: Math.abs(weights[i]);
			totalWeight += weight;
			value += dqs[i] * weight;
		}

		if (totalWeight == 0)
			return 0;
		value /= totalWeight;
		int m = setup.roundingMode == RoundingMode.UP
				? Math.round((float) Math.ceil(value))
				: Math.round((float) value);
		return Math.min(m, max);
	}

	private static class BMatrix {

		final int rows;
		final int columns;
		private final byte[] data;

		BMatrix(int rows, int columns) {
			this.rows = rows;
			this.columns = columns;
			this.data = new byte[rows * columns];
		}

		void set(int row, int col, int value) {
			data[index(row, col)] = (byte) value;
		}

		int get(int row, int col) {
			return data[index(row, col)];
		}

		int index(int row, int column) {
			return row + rows * column;
		}
	}
}
