package com.qtsp.dss.internal.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtsp.dss.internal.app.AppContext;
import com.qtsp.dss.internal.app.ServiceConfig;
import com.qtsp.dss.internal.model.ValidationReportsBundleResponse;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public void filter(ContainerRequestContext requestContext) {
		ServiceConfig config = AppContext.getConfig();
		if (!config.isRequireAuth()) {
			return;
		}

		String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			abort(requestContext, "Missing Authorization header");
			return;
		}

		String token = authorization.substring("Bearer ".length()).trim();
		if (token.isEmpty()) {
			abort(requestContext, "Missing bearer token");
			return;
		}

		if (config.isVerifyGoogleToken()) {
			if (!verifyToken(token, config.getAuthAudience())) {
				abort(requestContext, "Invalid ID token");
			}
		}
	}

	private boolean verifyToken(String token, String expectedAudience) {
		try {
			String encoded = URLEncoder.encode(token, "UTF-8");
			URL url = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=" + encoded);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			int code = connection.getResponseCode();
			if (code != 200) {
				return false;
			}
			try (InputStream inputStream = connection.getInputStream()) {
				JsonNode node = MAPPER.readTree(inputStream);
				String audience = node.path("aud").asText(null);
				if (expectedAudience != null && audience != null) {
					return expectedAudience.equals(audience);
				}
				return expectedAudience == null || expectedAudience.isEmpty();
			}
		} catch (Exception e) {
			return false;
		}
	}

	private void abort(ContainerRequestContext requestContext, String message) {
		ValidationReportsBundleResponse response = ValidationReportsBundleResponse.errorWithMessage(message);
		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
				.type(MediaType.APPLICATION_JSON)
				.entity(response)
				.build());
	}
}
