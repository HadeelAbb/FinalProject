package com.hsts.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Server-assembled Principal comparison: one request, all related exam rows. */
public class PrincipalComparisonReport implements Serializable {

    private PrincipalReportType reportType;
    private String filterValue;
    private List<PrincipalComparisonRow> rows = new ArrayList<>();

    public PrincipalComparisonReport() {
    }

    public PrincipalComparisonReport(PrincipalReportType reportType, String filterValue,
                                     List<PrincipalComparisonRow> rows) {
        this.reportType = reportType;
        this.filterValue = filterValue;
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public PrincipalReportType getReportType() {
        return reportType;
    }

    public void setReportType(PrincipalReportType reportType) {
        this.reportType = reportType;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    public List<PrincipalComparisonRow> getRows() {
        return rows;
    }

    public void setRows(List<PrincipalComparisonRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
