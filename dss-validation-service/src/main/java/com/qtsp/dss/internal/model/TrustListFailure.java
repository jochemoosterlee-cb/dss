package com.qtsp.dss.internal.model;

public class TrustListFailure {
	private String type;
	private String url;
	private String stage;
	private String message;

	public TrustListFailure() {
	}

	public TrustListFailure(String type, String url, String stage, String message) {
		this.type = type;
		this.url = url;
		this.stage = stage;
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
