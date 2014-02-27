package org.openlca.io.refdata;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.supercsv.io.CsvListWriter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class ImpactCategoryExport extends Export {

	@Override
	protected void doIt(CsvListWriter writer, IDatabase database) throws Exception {
		log.trace("write impact categories");
		String query = "select c.ref_id, c.name, c.description, c.reference_unit, " +
				"m.ref_id from tbl_impact_categories c join tbl_impact_methods m " +
				"on c.f_impact_method = m.id";
		final AtomicInteger count = new AtomicInteger(0);
		NativeSql.on(database).query(query, new NativeSql.QueryResultHandler() {
			@Override
			public boolean nextResult(ResultSet resultSet) throws SQLException {
				Object[] line = createLine(resultSet);
				count.incrementAndGet();
				return true;
			}
		});
		log.trace("{} impact categories written", count.get());
	}

	private Object[] createLine(ResultSet resultSet) throws SQLException {
		Object[] line = new Object[5];
		line[0] = resultSet.getString(1);
		line[1] = resultSet.getString(2);
		line[2] = resultSet.getString(3);
		line[3] = resultSet.getString(4);
		line[4] = resultSet.getString(5);
		return line;
	}
}
