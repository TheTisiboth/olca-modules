package org.openlca.core.results;

import java.util.List;

import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.ImpactIndex;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;

/**
 * The common protocol of all result types.
 */
public interface IResult {

	/**
	 * The index $\mathit{Idx}_A$ of the technology matrix $\mathbf{A}$. It maps the
	 * process-product pairs (or process-waste pairs) $\mathit{P}$ of the product
	 * system to the respective $n$ rows and columns of $\mathbf{A}$. If the product
	 * system contains other product systems as sub-systems, these systems are
	 * handled like processes and are also mapped as pair with their quantitative
	 * reference flow to that index (and also their processes etc.).
	 * <p>
	 * $$\mathit{Idx}_A: \mathit{P} \mapsto [0 \dots n-1]$$
	 */
	TechIndex techIndex();

	/**
	 * The row index $\mathit{Idx}_B$ of the intervention matrix $\mathbf{B}$. It
	 * maps the (elementary) flows $\mathit{F}$ of the processes in the product
	 * system to the $k$ rows of $\mathbf{B}$.
	 * <p>
	 * $$\mathit{Idx}_B: \mathit{F} \mapsto [0 \dots k-1]$$
	 */
	FlowIndex flowIndex();

	/**
	 * The row index $\mathit{Idx}_C$ of the matrix with the characterization
	 * factors $\mathbf{C}$. It maps the LCIA categories $\mathit{C}$ to the $l$
	 * rows of $\mathbf{C}$.
	 * <p>
	 * $$\mathit{Idx}_C: \mathit{C} \mapsto [0 \dots l-1]$$
	 */
	ImpactIndex impactIndex();

	/**
	 * Returns true when this result contains (elementary) flow results.
	 */
	default boolean hasFlowResults() {
		var flowIndex = flowIndex();
		return flowIndex != null && !flowIndex.isEmpty();
	}

	/**
	 * Returns true when this result contains LCIA results.
	 */
	default boolean hasImpactResults() {
		var impactIndex = impactIndex();
		return impactIndex != null && !impactIndex.isEmpty();
	}

	/**
	 * Returns true when this result contains LCC results.
	 */
	boolean hasCostResults();

	/**
	 * Get the descriptors of the processes of the inventory model. If a product
	 * system contains other product systems, these sub-systems are also handled
	 * like processes and returned.
	 */
	List<CategorizedDescriptor> getProcesses();

	/**
	 * Returns the list of flows that are mapped to the row index of the
	 * intervention matrix. Typically, these are the elementary flows of the LCA
	 * model. The returned list is part of the result and should be never modified
	 * but reordering via sorting is allowed.
	 */
	List<IndexFlow> getFlows();

	/**
	 * Get the LCIA categories of the LCIA result.
	 */
	List<ImpactDescriptor> getImpacts();


	/**
	 * Switches the sign for input-flows.
	 */
	default double adopt(IndexFlow flow, double value) {
		if (flow == null || !flow.isInput)
			return value;
		// avoid -0 in the results
		return value == 0 ? 0 : -value;
	}
}
