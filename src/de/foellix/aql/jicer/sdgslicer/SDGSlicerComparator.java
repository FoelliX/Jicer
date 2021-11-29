package de.foellix.aql.jicer.sdgslicer;

import java.util.Comparator;

public class SDGSlicerComparator implements Comparator<SDGSlicer> {
	private static SDGSlicerComparator instance = new SDGSlicerComparator();

	private SDGSlicerComparator() {
	}

	public static SDGSlicerComparator getInstance() {
		return instance;
	}

	@Override
	public int compare(SDGSlicer slicer1, SDGSlicer slicer2) {
		final GlobalVisit globalVisit1 = slicer1.getGlobalVisit();
		final GlobalVisit globalVisit2 = slicer2.getGlobalVisit();
		if (globalVisit1.getContextSensitivity().size() > globalVisit2.getContextSensitivity().size()) {
			return -1;
		} else if (globalVisit1.getContextSensitivity().size() < globalVisit2.getContextSensitivity().size()) {
			return 1;
		}
		return 0;
	}
}