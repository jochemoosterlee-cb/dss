package com.qtsp.dss.internal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidationReportsBundleResponse {
	private String simpleReportXml;
	private String detailedReportXml;
	private String diagnosticDataXml;
	private String etsiValidationReportXml;
	private List<String> errors = new ArrayList<>();

	public String getSimpleReportXml() {
		return simpleReportXml;
	}

	public void setSimpleReportXml(String simpleReportXml) {
		this.simpleReportXml = simpleReportXml;
	}

	public String getDetailedReportXml() {
		return detailedReportXml;
	}

	public void setDetailedReportXml(String detailedReportXml) {
		this.detailedReportXml = detailedReportXml;
	}

	public String getDiagnosticDataXml() {
		return diagnosticDataXml;
	}

	public void setDiagnosticDataXml(String diagnosticDataXml) {
		this.diagnosticDataXml = diagnosticDataXml;
	}

	public String getEtsiValidationReportXml() {
		return etsiValidationReportXml;
	}

	public void setEtsiValidationReportXml(String etsiValidationReportXml) {
		this.etsiValidationReportXml = etsiValidationReportXml;
	}

	public List<String> getErrors() {
		return errors == null ? new ArrayList<>() : errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors == null ? new ArrayList<>() : errors;
	}

	public static ValidationReportsBundleResponse errorWithMessage(String message) {
		ValidationReportsBundleResponse response = new ValidationReportsBundleResponse();
		if (message != null && !message.trim().isEmpty()) {
			response.setErrors(Arrays.asList(message.trim()));
		}
		return response;
	}
}
