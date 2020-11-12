package org.openlca.core.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_locations")
public class Location extends CategorizedEntity {

	@Column(name = "code")
	public String code;

	@Column(name = "latitude")
	public double latitude;

	@Column(name = "longitude")
	public double longitude;

	/**
	 * Contains the geographic data of this location in a binary format. We
	 * convert GeoJSON to a Protocol Buffers format and then compress this via
	 * gzip when writing this information and the other way around when reading
	 * it in openLCA.
	 */
	@Lob
	@Column(name = "geodata")
	public byte[] geodata;

	public static Location of(String name) {
		return Location.of(name, name);
	}

	public static Location of(String name, String code) {
		var location = new Location();
		Entities.init(location, name);
		location.code = code;
		return location;
	}

	@Override
	public Location clone() {
		var clone = new Location();
		Entities.copyFields(this, clone);
		clone.code = code;
		clone.latitude = latitude;
		clone.longitude = longitude;
		clone.geodata = geodata;
		return clone;
	}
}
