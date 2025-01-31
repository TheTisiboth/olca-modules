package org.openlca.core.matrix.product.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.matrix.LinkingConfig.DefaultProviders;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.matrix.cache.ProcessTable;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessType;

/**
 * Searches for the best provider for a given product input or waste output in
 * the database.
 */
public class ProviderSearch {

	private final ProcessTable processTable;
	private final LinkingConfig config;

	public ProviderSearch(ProcessTable processTable, LinkingConfig config) {
		this.processTable = processTable;
		this.config = config;
	}

	/**
	 * Find the best provider for the given product input or waste output
	 * according to the search settings.
	 */
	public ProcessProduct find(CalcExchange e) {
		if (e == null || cancel())
			return null;
		List<ProcessProduct> providers = processTable.getProviders(e.flowId);
		if (providers.isEmpty())
			return null;

		// select a default provider if present
		// this needs to be done before asking a potential callback
		// for options as the callback should be only called when
		// there are multiple options.
		if (config.providerLinking != DefaultProviders.IGNORE) {
			for (ProcessProduct provider : providers) {
				if (provider.processId() == e.defaultProviderId)
					return provider;
			}
			if (config.providerLinking == DefaultProviders.ONLY)
				return null;
		}

		// check form single options and callback
		if (providers.size() == 1)
			return providers.get(0);
		if (config.callback != null) {
			providers = config.callback.select(e, providers);
			if (providers == null || providers.size() == 0)
				return null;
			if (providers.size() == 1)
				return providers.get(0);
		}

		ProcessProduct candidate = null;
		for (ProcessProduct next : providers) {
			if (isBetter(e, candidate, next)) {
				candidate = next;
			}
		}
		return candidate;
	}

	private boolean isBetter(CalcExchange e, ProcessProduct old, ProcessProduct newOption) {
		if (old == null)
			return true;
		if (newOption == null)
			return false;
		if (config.providerLinking != DefaultProviders.IGNORE) {
			if (old.processId() == e.defaultProviderId)
				return false;
			if (newOption.processId() == e.defaultProviderId)
				return true;
		}
		ProcessType oldType = processTable.getType(old.processId());
		ProcessType newType = processTable.getType(newOption.processId());
		if (oldType == config.preferredType
				&& newType != config.preferredType)
			return false;
		return oldType != config.preferredType
				&& newType == config.preferredType;
	}

	/**
	 * Returns from the given list the product inputs or waste outputs that
	 * could be linked to a provider.
	 */
	public List<CalcExchange> getLinkCandidates(List<CalcExchange> list) {
		if (list == null || list.isEmpty() || cancel())
			return Collections.emptyList();
		List<CalcExchange> candidates = new ArrayList<>();
		for (CalcExchange e : list) {
			if (config.providerLinking == DefaultProviders.ONLY
					&& e.defaultProviderId == 0L)
				continue;
			if (e.flowType == null || e.flowType == FlowType.ELEMENTARY_FLOW)
				continue;
			if (e.isInput && e.flowType == FlowType.PRODUCT_FLOW) {
				candidates.add(e);
			} else if (!e.isInput && e.flowType == FlowType.WASTE_FLOW) {
				candidates.add(e);
			}
		}
		return candidates;
	}

	private boolean cancel() {
		return config.callback != null
				&& config.callback.cancel();
	}

	ProcessProduct getProvider(long id, long flowId) {
		return processTable.getProvider(id, flowId);
	}

}
