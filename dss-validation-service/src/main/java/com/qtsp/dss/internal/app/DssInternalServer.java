package com.qtsp.dss.internal.app;

import com.qtsp.dss.internal.api.AuthFilter;
import com.qtsp.dss.internal.api.HealthResource;
import com.qtsp.dss.internal.api.ValidationResource;
import com.qtsp.dss.internal.api.TrustListResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public final class DssInternalServer {
	private static final Logger LOG = LoggerFactory.getLogger(DssInternalServer.class);

	public static void main(String[] args) throws Exception {
		ServiceConfig config = ServiceConfig.fromEnv();
		AppContext.init(config);

		URI baseUri = URI.create("http://0.0.0.0:" + config.getPort() + "/");
		ResourceConfig resourceConfig = new ResourceConfig()
				.register(ValidationResource.class)
				.register(TrustListResource.class)
				.register(HealthResource.class)
				.register(AuthFilter.class)
				.register(MultiPartFeature.class)
				.register(JacksonFeature.class);

		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

		server.start();
		LOG.info("DSS internal service started on {}", baseUri);

		Thread.currentThread().join();
	}
}
