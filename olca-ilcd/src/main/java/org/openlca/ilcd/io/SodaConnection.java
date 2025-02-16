package org.openlca.ilcd.io;

import java.util.Objects;

/**
 * Connection data of an Soda4LCA service.
 */
public class SodaConnection {

	public String uuid;

	/**
	 * The URL of the service end-point.
	 */
	public String url;

	public String user;

	public String password;

	public String dataStockId;

	/**
	 * The short name of the data stock; just used for display information.
	 */
	public String dataStockName;

	@Override
	public String toString() {
		String s = url;
		if (dataStockName != null && dataStockName.length() > 0) {
			s = dataStockName + "::" + url;
		}
		return user != null && user.length() > 0
			? user + "@" + url
			: s;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof SodaConnection))
			return false;
		var other = (SodaConnection) obj;
		return Objects.equals(this.id(), other.id());
	}

	private String id() {
		if (uuid != null)
			return uuid;
		else
			return toString();
	}

}
