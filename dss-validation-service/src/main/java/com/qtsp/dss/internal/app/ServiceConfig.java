package com.qtsp.dss.internal.app;

public final class ServiceConfig {
	private final int port;
	private final int maxUploadBytes;
	private final boolean requireAuth;
	private final boolean verifyGoogleToken;
	private final String authAudience;
	private final String lotlUrl;
	private final String nlTlUrl;
	private final String ojUrl;
	private final String lotlKeystoreResource;
	private final String lotlKeystorePassword;
	private final boolean trustListRefreshOnStart;
	private final int trustListRefreshIntervalMinutes;

	private ServiceConfig(int port, int maxUploadBytes, boolean requireAuth, boolean verifyGoogleToken,
					  String authAudience, String lotlUrl, String nlTlUrl, String ojUrl, String lotlKeystoreResource,
					  String lotlKeystorePassword, boolean trustListRefreshOnStart, int trustListRefreshIntervalMinutes) {
		this.port = port;
		this.maxUploadBytes = maxUploadBytes;
		this.requireAuth = requireAuth;
		this.verifyGoogleToken = verifyGoogleToken;
		this.authAudience = authAudience;
		this.lotlUrl = lotlUrl;
		this.nlTlUrl = nlTlUrl;
		this.ojUrl = ojUrl;
		this.lotlKeystoreResource = lotlKeystoreResource;
		this.lotlKeystorePassword = lotlKeystorePassword;
		this.trustListRefreshOnStart = trustListRefreshOnStart;
		this.trustListRefreshIntervalMinutes = trustListRefreshIntervalMinutes;
	}

	public static ServiceConfig fromEnv() {
		int port = parseInt(getEnv("PORT"), 8080);
		int maxUploadBytes = parseInt(getEnv("DSS_MAX_UPLOAD_BYTES"), 25 * 1024 * 1024);
		boolean requireAuth = parseBoolean(getEnv("DSS_REQUIRE_AUTH"), true);
		boolean verifyGoogleToken = parseBoolean(getEnv("DSS_VERIFY_GOOGLE_TOKEN"), true);
		String authAudience = trimToNull(getEnv("DSS_AUTH_AUDIENCE"));
		String lotlUrl = getOrDefault("DSS_LOTL_URL", "https://ec.europa.eu/tools/lotl/eu-lotl.xml");
		String nlTlUrl = getOrDefault("DSS_TL_NL_URL", "https://download.eidasa.europa.eu/tsl/tsl-NL.xml");
		String ojUrl = getOrDefault("DSS_OJ_URL", "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=uriserv:OJ.C_.2019.276.01.0001.01.ENG");
		String lotlKeystoreResource = getOrDefault("DSS_LOTL_KEYSTORE_RESOURCE", "keystore.p12");
		String lotlKeystorePassword = getOrDefault("DSS_LOTL_KEYSTORE_PASSWORD", "dss-password");
		boolean trustListRefreshOnStart = parseBoolean(getEnv("DSS_TRUST_LIST_REFRESH_ON_START"), false);
		int trustListRefreshIntervalMinutes = parseInt(getEnv("DSS_TRUST_LIST_REFRESH_INTERVAL_MINUTES"), 0);
		return new ServiceConfig(port, maxUploadBytes, requireAuth, verifyGoogleToken, authAudience, lotlUrl, nlTlUrl,
				ojUrl, lotlKeystoreResource, lotlKeystorePassword, trustListRefreshOnStart, trustListRefreshIntervalMinutes);
	}

	public int getPort() {
		return port;
	}

	public int getMaxUploadBytes() {
		return maxUploadBytes;
	}

	public boolean isRequireAuth() {
		return requireAuth;
	}

	public boolean isVerifyGoogleToken() {
		return verifyGoogleToken;
	}

	public String getAuthAudience() {
		return authAudience;
	}

	public String getLotlUrl() {
		return lotlUrl;
	}

	public String getNlTlUrl() {
		return nlTlUrl;
	}

	public String getOjUrl() {
		return ojUrl;
	}

	public String getLotlKeystoreResource() {
		return lotlKeystoreResource;
	}

	public String getLotlKeystorePassword() {
		return lotlKeystorePassword;
	}

	public boolean isTrustListRefreshOnStart() {
		return trustListRefreshOnStart;
	}

	public int getTrustListRefreshIntervalMinutes() {
		return trustListRefreshIntervalMinutes;
	}

	private static String getEnv(String name) {
		return System.getenv(name);
	}

	private static String getOrDefault(String name, String defaultValue) {
		String value = getEnv(name);
		return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static int parseInt(String value, int defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		return Boolean.parseBoolean(value.trim());
	}
}
