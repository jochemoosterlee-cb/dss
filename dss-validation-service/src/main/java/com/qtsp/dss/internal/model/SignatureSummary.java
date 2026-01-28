package com.qtsp.dss.internal.model;

import java.util.ArrayList;
import java.util.List;

public class SignatureSummary {
	private String id;
	private String status;
	private String signingTime;
	private String signer;
	private String revocation;
	private String trust;
	private List<String> substatus = new ArrayList<>();

	public SignatureSummary() {
		// jackson
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSigningTime() {
		return signingTime;
	}

	public void setSigningTime(String signingTime) {
		this.signingTime = signingTime;
	}

	public String getSigner() {
		return signer;
	}

	public void setSigner(String signer) {
		this.signer = signer;
	}

	public String getRevocation() {
		return revocation;
	}

	public void setRevocation(String revocation) {
		this.revocation = revocation;
	}

	public String getTrust() {
		return trust;
	}

	public void setTrust(String trust) {
		this.trust = trust;
	}

	public List<String> getSubstatus() {
		return substatus;
	}

	public void setSubstatus(List<String> substatus) {
		this.substatus = substatus;
	}
}
