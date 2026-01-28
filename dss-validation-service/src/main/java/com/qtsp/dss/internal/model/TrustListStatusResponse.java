package com.qtsp.dss.internal.model;

public class TrustListStatusResponse {
	private String lotlUrl;
	private String nlTlUrl;
	private String lastRefreshAttemptAt;
	private String lastRefreshSuccessAt;
	private String lastRefreshError;
	private boolean refreshInProgress;
	private boolean refreshFailed;
	private int trustedEntities;

	public String getLotlUrl() {
		return lotlUrl;
	}

	public void setLotlUrl(String lotlUrl) {
		this.lotlUrl = lotlUrl;
	}

	public String getNlTlUrl() {
		return nlTlUrl;
	}

	public void setNlTlUrl(String nlTlUrl) {
		this.nlTlUrl = nlTlUrl;
	}

	public String getLastRefreshAttemptAt() {
		return lastRefreshAttemptAt;
	}

	public void setLastRefreshAttemptAt(String lastRefreshAttemptAt) {
		this.lastRefreshAttemptAt = lastRefreshAttemptAt;
	}

	public String getLastRefreshSuccessAt() {
		return lastRefreshSuccessAt;
	}

	public void setLastRefreshSuccessAt(String lastRefreshSuccessAt) {
		this.lastRefreshSuccessAt = lastRefreshSuccessAt;
	}

	public String getLastRefreshError() {
		return lastRefreshError;
	}

	public void setLastRefreshError(String lastRefreshError) {
		this.lastRefreshError = lastRefreshError;
	}

	public boolean isRefreshInProgress() {
		return refreshInProgress;
	}

	public void setRefreshInProgress(boolean refreshInProgress) {
		this.refreshInProgress = refreshInProgress;
	}

	public boolean isRefreshFailed() {
		return refreshFailed;
	}

	public void setRefreshFailed(boolean refreshFailed) {
		this.refreshFailed = refreshFailed;
	}

	public int getTrustedEntities() {
		return trustedEntities;
	}

	public void setTrustedEntities(int trustedEntities) {
		this.trustedEntities = trustedEntities;
	}
}
