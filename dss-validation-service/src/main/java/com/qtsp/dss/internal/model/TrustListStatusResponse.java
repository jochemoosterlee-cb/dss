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
	private String refreshPhase;
	private String refreshStartedAt;
	private String lastProgressAt;
	private Integer processedLotlCount;
	private Integer processedTlCount;
	private Integer totalLotlCountEstimate;
	private Integer totalTlCountEstimate;
	private Integer refreshFailureCount;
	private java.util.List<TrustListFailure> refreshFailures;

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

	public String getRefreshPhase() {
		return refreshPhase;
	}

	public void setRefreshPhase(String refreshPhase) {
		this.refreshPhase = refreshPhase;
	}

	public String getRefreshStartedAt() {
		return refreshStartedAt;
	}

	public void setRefreshStartedAt(String refreshStartedAt) {
		this.refreshStartedAt = refreshStartedAt;
	}

	public String getLastProgressAt() {
		return lastProgressAt;
	}

	public void setLastProgressAt(String lastProgressAt) {
		this.lastProgressAt = lastProgressAt;
	}

	public Integer getProcessedLotlCount() {
		return processedLotlCount;
	}

	public void setProcessedLotlCount(Integer processedLotlCount) {
		this.processedLotlCount = processedLotlCount;
	}

	public Integer getProcessedTlCount() {
		return processedTlCount;
	}

	public void setProcessedTlCount(Integer processedTlCount) {
		this.processedTlCount = processedTlCount;
	}

	public Integer getTotalLotlCountEstimate() {
		return totalLotlCountEstimate;
	}

	public void setTotalLotlCountEstimate(Integer totalLotlCountEstimate) {
		this.totalLotlCountEstimate = totalLotlCountEstimate;
	}

	public Integer getTotalTlCountEstimate() {
		return totalTlCountEstimate;
	}

	public void setTotalTlCountEstimate(Integer totalTlCountEstimate) {
		this.totalTlCountEstimate = totalTlCountEstimate;
	}

	public Integer getRefreshFailureCount() {
		return refreshFailureCount;
	}

	public void setRefreshFailureCount(Integer refreshFailureCount) {
		this.refreshFailureCount = refreshFailureCount;
	}

	public java.util.List<TrustListFailure> getRefreshFailures() {
		return refreshFailures;
	}

	public void setRefreshFailures(java.util.List<TrustListFailure> refreshFailures) {
		this.refreshFailures = refreshFailures;
	}
}
