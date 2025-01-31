package org.openlca.core.matrix.product.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TechIndexBuilder implements ITechIndexBuilder {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ProviderSearch providers;
	private final MatrixCache cache;
	private final ProductSystem system;

	public TechIndexBuilder(MatrixCache cache, ProductSystem system,
			LinkingConfig config) {
		this.cache = cache;
		this.system = system;
		this.providers = new ProviderSearch(cache.getProcessTable(), config);
	}

	@Override
	public TechIndex build(ProcessProduct refProduct) {
		return build(refProduct, 1.0);
	}

	@Override
	public TechIndex build(ProcessProduct refFlow, double demand) {
		log.trace("build product index for {}", refFlow);
		TechIndex index = new TechIndex(refFlow);
		index.setDemand(demand);
		addSystemLinks(index);
		List<ProcessProduct> block = new ArrayList<>();
		block.add(refFlow);
		HashSet<ProcessProduct> handled = new HashSet<>();
		while (!block.isEmpty()) {
			List<ProcessProduct> nextBlock = new ArrayList<>();
			log.trace("fetch next block with {} entries", block.size());
			Map<Long, List<CalcExchange>> exchanges = fetchExchanges(block);
			for (ProcessProduct recipient : block) {
				handled.add(recipient);
				List<CalcExchange> all = exchanges.get(recipient.processId());
				List<CalcExchange> candidates = providers
						.getLinkCandidates(all);
				for (CalcExchange linkExchange : candidates) {
					ProcessProduct provider = providers.find(linkExchange);
					if (provider == null)
						continue;
					LongPair exchange = new LongPair(recipient.processId(),
							linkExchange.exchangeId);
					index.putLink(exchange, provider);
					if (!handled.contains(provider)
							&& !nextBlock.contains(provider))
						nextBlock.add(provider);
				}
			}
			block = nextBlock;
		}
		return index;
	}

	private void addSystemLinks(TechIndex index) {
		if (system == null)
			return;
		for (ProcessLink link : system.processLinks) {
			ProcessProduct provider = providers.getProvider(
					link.providerId, link.flowId);
			if (provider == null)
				continue;
			LongPair exchange = new LongPair(link.processId, link.exchangeId);
			index.putLink(exchange, provider);
		}
	}

	private Map<Long, List<CalcExchange>> fetchExchanges(List<ProcessProduct> block) {
		if (block.isEmpty())
			return Collections.emptyMap();
		Set<Long> processIds = new HashSet<>();
		for (ProcessProduct provider : block) {
			processIds.add(provider.processId());
		}
		try {
			return cache.getExchangeCache().getAll(processIds);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to load exchanges from cache", e);
			return Collections.emptyMap();
		}
	}

}
