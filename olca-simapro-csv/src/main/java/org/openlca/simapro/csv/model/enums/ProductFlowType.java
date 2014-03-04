package org.openlca.simapro.csv.model.enums;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allowed types of product flows in SimaPro. In a SimaPro CSV file, product
 * exchanges are divided into sections of these types where each section starts
 * with the CSV header indicating the type.
 */
public enum ProductFlowType {

	PRODUCTS("Products"),

	AVOIDED_PRODUCTS("Avoided products"),

	ELECTRICITY_HEAT("Electricity/heat"),

	MATERIAL_FUELS("Materials/fuels"),

	WASTE_TO_TREATMENT("Waste to treatment");

	private final String header;

	private ProductFlowType(String header) {
		this.header = header;
	}

	public String getHeader() {
		return header;
	}

	public static ProductFlowType forHeader(String header) {
		for (ProductFlowType type : values())
			if (type.getHeader().equalsIgnoreCase(header))
				return type;
		Logger log = LoggerFactory.getLogger(ProductFlowType.class);
		log.warn("unknown product type header {}; returning NULL", header);
		return null;
	}

}
