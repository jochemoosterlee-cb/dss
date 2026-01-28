package com.qtsp.dss.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationSummaryResponse {
	private String overall;
	private int signatureCount;
	private List<SignatureSummary> signatures = new ArrayList<>();
	private List<String> errors = new ArrayList<>();

	public ValidationSummaryResponse() {
		// jackson
	}

	public ValidationSummaryResponse(String overall, int signatureCount, List<SignatureSummary> signatures, List<String> errors) {
		this.overall = overall;
		this.signatureCount = signatureCount;
		this.signatures = signatures == null ? new ArrayList<SignatureSummary>() : signatures;
		this.errors = errors == null ? new ArrayList<String>() : errors;
	}

	public static ValidationSummaryResponse indeterminateWithError(String message) {
		ValidationSummaryResponse response = new ValidationSummaryResponse();
		response.overall = "INDETERMINATE";
		response.signatureCount = 0;
		response.signatures = new ArrayList<>();
		response.errors = new ArrayList<>();
		if (message != null && !message.trim().isEmpty()) {
			response.errors.add(message.trim());
		}
		return response;
	}

	public String getOverall() {
		return overall;
	}

	public void setOverall(String overall) {
		this.overall = overall;
	}

	public int getSignatureCount() {
		return signatureCount;
	}

	public void setSignatureCount(int signatureCount) {
		this.signatureCount = signatureCount;
	}

	public List<SignatureSummary> getSignatures() {
		return signatures == null ? Collections.<SignatureSummary>emptyList() : signatures;
	}

	public void setSignatures(List<SignatureSummary> signatures) {
		this.signatures = signatures;
	}

	public List<String> getErrors() {
		return errors == null ? Collections.<String>emptyList() : errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
}
