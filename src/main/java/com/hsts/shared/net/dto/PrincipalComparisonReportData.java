package com.hsts.shared.net.dto;

import com.hsts.shared.model.PrincipalReportType;

import java.io.Serializable;

/**
 * Principal comparison request. filterValue is a resource id (teacher, course, or student).
 * No principal identity is sent or trusted.
 */
public class PrincipalComparisonReportData implements Serializable {

    private PrincipalReportType reportType;
    private String filterValue;

    public PrincipalComparisonReportData() {
    }

    public PrincipalComparisonReportData(PrincipalReportType reportType, String filterValue) {
        this.reportType = reportType;
        this.filterValue = filterValue;
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
}
