package org.openlca.core.database.references;

import java.util.List;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.references.Search.Ref;
import org.openlca.core.model.Category;
import org.openlca.core.model.Currency;
import org.openlca.core.model.descriptors.CurrencyDescriptor;

public class CurrencyReferenceSearch extends
		BaseReferenceSearch<CurrencyDescriptor> {

	private final static Ref[] references = { 
		new Ref(Category.class, "f_category", true), 
		new Ref(Currency.class, "f_reference_currency") 
	};

	public CurrencyReferenceSearch(IDatabase database, boolean includeOptional) {
		super(database, includeOptional);
	}

	@Override
	public List<Reference> findReferences(Set<Long> ids) {
		return findReferences("tbl_currencies", "id", ids, references);
	}

}
