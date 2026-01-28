package com.qtsp.dss.internal.validation;

public class ValidationRequestOptions {
	private final boolean requireRevocation;
	private final String mode;
	private final String policy;
	private final String trustList;

	private ValidationRequestOptions(boolean requireRevocation, String mode, String policy, String trustList) {
		this.requireRevocation = requireRevocation;
		this.mode = mode;
		this.policy = policy;
		this.trustList = trustList;
	}

	public static ValidationRequestOptions from(String mode, String policy, String requireRevocation, String trustList) {
		String resolvedMode = normalize(mode, "summary");
		if (!"summary".equalsIgnoreCase(resolvedMode)) {
			throw new ValidationException(400, "Unsupported mode. Only 'summary' is allowed.");
		}

		String resolvedPolicy = normalize(policy, "default");
		if (!"default".equalsIgnoreCase(resolvedPolicy)) {
			throw new ValidationException(400, "Unsupported policy. Only 'default' is allowed.");
		}

		String resolvedTrustList = normalize(trustList, "eu");
		if (!"eu".equalsIgnoreCase(resolvedTrustList)) {
			throw new ValidationException(400, "Unsupported trustList. Only 'eu' is allowed.");
		}

		boolean resolvedRequireRevocation = parseBoolean(requireRevocation, true);

		return new ValidationRequestOptions(resolvedRequireRevocation, resolvedMode.toLowerCase(),
				resolvedPolicy.toLowerCase(), resolvedTrustList.toLowerCase());
	}

	public boolean isRequireRevocation() {
		return requireRevocation;
	}

	public String getMode() {
		return mode;
	}

	public String getPolicy() {
		return policy;
	}

	public String getTrustList() {
		return trustList;
	}

	private static String normalize(String value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? defaultValue : trimmed;
	}

	private static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		String normalized = value.trim().toLowerCase();
		if ("true".equals(normalized)) {
			return true;
		}
		if ("false".equals(normalized)) {
			return false;
		}
		throw new ValidationException(400, "requireRevocation must be 'true' or 'false'.");
	}
}
