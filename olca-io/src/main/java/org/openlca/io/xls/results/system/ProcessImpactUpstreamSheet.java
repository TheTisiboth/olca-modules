package org.openlca.io.xls.results.system;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResultProvider;
import org.openlca.io.xls.results.CellWriter;

class ProcessImpactUpstreamSheet
		extends ContributionSheet<ProcessDescriptor, ImpactCategoryDescriptor> {

	private final CellWriter writer;
	private final FullResultProvider result;

	static void write(ResultExport export,
			FullResultProvider result) {
		new ProcessImpactUpstreamSheet(export, result)
				.write(export.workbook, export.processes, export.impacts);
	}

	private ProcessImpactUpstreamSheet(ResultExport export,
			FullResultProvider result) {
		super(export.writer, ResultExport.PROCESS_HEADER,
				ResultExport.FLOW_HEADER);
		this.writer = export.writer;
		this.result = result;
	}

	private void write(Workbook workbook, List<ProcessDescriptor> processes,
			List<ImpactCategoryDescriptor> impacts) {
		Sheet sheet = workbook.createSheet("Process upstream impacts");
		header(sheet);
		subHeaders(sheet, processes, impacts);
		data(sheet, processes, impacts);
	}

	@Override
	protected double getValue(ProcessDescriptor process,
			ImpactCategoryDescriptor impact) {
		return result.getUpstreamImpactResult(process, impact).value;
	}

	@Override
	protected void subHeaderCol(ProcessDescriptor process, Sheet sheet,
			int col) {
		writer.processCol(sheet, 1, col, process);
	}

	@Override
	protected void subHeaderRow(ImpactCategoryDescriptor impact, Sheet sheet,
			int row) {
		writer.impactRow(sheet, row, 1, impact);
	}
}
