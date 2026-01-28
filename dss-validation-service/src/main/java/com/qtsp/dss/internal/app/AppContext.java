package com.qtsp.dss.internal.app;

import com.qtsp.dss.internal.validation.PdfValidationService;

public final class AppContext {
	private static ServiceConfig config;
	private static PdfValidationService validationService;
	private static java.util.concurrent.ScheduledExecutorService scheduler;

	private AppContext() {
		// static only
	}

	public static synchronized void init(ServiceConfig serviceConfig) {
		if (config != null) {
			return;
		}
		config = serviceConfig;
		validationService = new PdfValidationService(serviceConfig);
		startTrustListScheduler(serviceConfig, validationService);
	}

	public static ServiceConfig getConfig() {
		if (config == null) {
			throw new IllegalStateException("AppContext not initialized");
		}
		return config;
	}

	public static PdfValidationService getValidationService() {
		if (validationService == null) {
			throw new IllegalStateException("AppContext not initialized");
		}
		return validationService;
	}

	private static void startTrustListScheduler(ServiceConfig serviceConfig, PdfValidationService service) {
		int intervalMinutes = serviceConfig.getTrustListRefreshIntervalMinutes();
		if (intervalMinutes <= 0) {
			return;
		}
		long initialDelay = serviceConfig.isTrustListRefreshOnStart() ? 0 : intervalMinutes;
		scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "dss-trustlist-refresh");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleAtFixedRate(service::refreshTrustLists, initialDelay, intervalMinutes,
				java.util.concurrent.TimeUnit.MINUTES);
	}
}
