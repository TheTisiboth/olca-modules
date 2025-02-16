package org.openlca.core.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.providers.ResultProvider;
import org.openlca.core.results.providers.ResultProviders;

/**
 * The `ContributionResult` extends the `SimpleResult` type. It also contains
 * all direct contributions of the processes to the LCI and LCIA results.
 * Additionally, it contains the contributions of the (elementary) flows to the
 * LCIA results.
 */
public class ContributionResult extends SimpleResult {

	public ContributionResult(ResultProvider provider) {
		super(provider);
	}

	public static ContributionResult of(IDatabase db, MatrixData data) {
		var provider = ResultProviders.lazyOf(db, data);
		return new ContributionResult(provider);
	}

	@Override
	public boolean hasFlowResults() {
		return provider.hasFlows();
	}

	@Override
	public boolean hasImpactResults() {
		return provider.hasImpacts();
	}

	@Override
	public boolean hasCostResults() {
		return provider.hasCosts();
	}

	/**
	 * Get the direct contribution of the given process-product pair $j$ to the
	 * inventory result of elementary flow $i$: $\mathbf{G}[i,j]$.
	 */
	public double getDirectFlowResult(ProcessProduct product, IndexFlow flow) {
		if (!hasFlowResults())
			return 0;
		int flowIdx = flowIndex().of(flow);
		int productIdx = techIndex().of(product);
		if (flowIdx < 0 || productIdx < 0)
			return 0;
		double value = provider.directFlowOf(flowIdx, productIdx);
		return adopt(flow, value);
	}

	/**
	 * Get the direct contribution of the given process $j$ to the inventory result
	 * of elementary flow $i$. When the process has multiple products it is the sum
	 * of the contributions of all of these process-product pairs.
	 */
	public double getDirectFlowResult(
			CategorizedDescriptor process, IndexFlow flow) {
		double total = 0;
		for (var p : techIndex().getProviders(process)) {
			total += getDirectFlowResult(p, flow);
		}
		return total;
	}

	/**
	 * Get the direct contributions of the given process $j$ to the inventory result
	 * of all elementary flows in the product system.
	 */
	public List<FlowResult> getFlowContributions(
			CategorizedDescriptor process) {
		if (!hasFlowResults())
			return Collections.emptyList();
		var results = new ArrayList<FlowResult>();
		flowIndex().each((i, flow) -> {
			double value = getDirectFlowResult(process, flow);
			results.add(new FlowResult(flow, value));
		});
		return results;
	}

	/**
	 * Get the direct contributions of the processes in the system to the inventory
	 * result of the given flow.
	 */
	public List<Contribution<CategorizedDescriptor>> getProcessContributions(
			IndexFlow flow) {
		return Contributions.calculate(
				getProcesses(),
				getTotalFlowResult(flow),
				d -> getDirectFlowResult(d, flow));
	}

	/**
	 * Get the direct contribution of the given process-product pair $j$ to the LCIA
	 * category result $j$: $\mathbf{D}[i,j]$.
	 */
	public double getDirectImpactResult(
			ProcessProduct product, ImpactDescriptor impact) {
		if (!hasImpactResults())
			return 0;
		int impactIdx = impactIndex().of(impact);
		int productIdx = techIndex().of(product);
		return impactIdx < 0 || productIdx < 0
				? 0
				: provider.directImpactOf(impactIdx, productIdx);
	}

	/**
	 * Get the direct contribution of the given process $j$ to the LCIA category
	 * result $i$. When the process has multiple products it is the sum of the
	 * contributions of all of these process-product pairs.
	 */
	public double getDirectImpactResult(
			CategorizedDescriptor process, ImpactDescriptor impact) {
		double total = 0;
		for (var p : techIndex().getProviders(process)) {
			total += getDirectImpactResult(p, impact);
		}
		return total;
	}

	/**
	 * Get the direct contributions of the given process $j$ to the LCIA category
	 * results.
	 */
	public List<ImpactResult> getImpactContributions(
			CategoryDescriptor process) {
		var results = new ArrayList<ImpactResult>();
		impactIndex().each((i, impact) -> {
			ImpactResult r = new ImpactResult();
			r.impact = impact;
			r.value = getDirectImpactResult(process, impact);
			results.add(r);
		});
		return results;
	}

	/**
	 * Get the direct contributions of the processes in the system to the LCIA
	 * result of the given LCIA category.
	 */
	public List<Contribution<CategorizedDescriptor>> getProcessContributions(
			ImpactDescriptor impact) {
		return Contributions.calculate(
				getProcesses(),
				getTotalImpactResult(impact),
				d -> getDirectImpactResult(d, impact));
	}

	/**
	 * Get the direct contribution of the given process-product pair $j$ to the LCC
	 * result: $\mathbf{k}_s[j]$.
	 */
	public double getDirectCostResult(ProcessProduct product) {
		int col = techIndex().of(product);
		return col < 0
				? 0
				: provider.directCostsOf(col);
	}

	/**
	 * Get the direct contribution of the given process $j$ to the LCC result. When
	 * the process has multiple products it is the sum of the contributions of all
	 * of these process-product pairs.
	 */
	public double getDirectCostResult(CategorizedDescriptor process) {
		double total = 0;
		for (var provider : techIndex().getProviders(process)) {
			total += getDirectCostResult(provider);
		}
		return total;
	}

	/**
	 * Get the direct contributions of all processes to the LCC result.
	 */
	public List<Contribution<CategorizedDescriptor>> getProcessCostContributions() {
		return Contributions.calculate(
				getProcesses(),
				totalCosts,
				this::getDirectCostResult);
	}

	/**
	 * Get the direct contribution of the given elementary flow to the LCIA result
	 * of the given LCIA category.
	 */
	public double getDirectFlowImpact(IndexFlow flow, ImpactDescriptor impact) {
		if (!hasImpactResults())
			return 0;
		int impactIdx = impactIndex().of(impact);
		int flowIdx = flowIndex().of(flow);
		return impactIdx < 0 || flowIdx < 0
				? 0
				: provider.flowImpactOf(impactIdx, flowIdx);
	}

	/**
	 * Get the contributions of all elementary flows to the given LCA category.
	 */
	public List<FlowResult> getFlowContributions(
			ImpactDescriptor impact) {
		var results = new ArrayList<FlowResult>();
		flowIndex().each((i, flow) -> {
			double value = getDirectFlowImpact(flow, impact);
			results.add(new FlowResult(flow, value));
		});
		return results;
	}

	/**
	 * Get the characterization factor for the given flow (and location in case of a
	 * regionalized result).
	 */
	public double getImpactFactor(
			ImpactDescriptor impact, IndexFlow flow) {
		if (!hasImpactResults())
			return 0;
		int impactIdx = impactIndex().of(impact);
		int flowIdx = flowIndex().of(flow);
		if (impactIdx < 0 || flowIdx < 0 )
			return 0;

		double value = provider.impactFactorOf(impactIdx, flowIdx);
		if (!flow.isInput)
			return value;

		// characterization factors for input flows are negative in the
		// matrix. A simple abs() is not correct because the original
		// characterization factor maybe was already negative (-(-(f))).
		return value == 0
				? 0 // avoid -0
				: -value;
	}
}
