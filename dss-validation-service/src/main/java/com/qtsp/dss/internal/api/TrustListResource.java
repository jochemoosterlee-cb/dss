package com.qtsp.dss.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtsp.dss.internal.app.AppContext;
import com.qtsp.dss.internal.model.TrustListStatusResponse;
import com.qtsp.dss.internal.validation.PdfValidationService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@Path("/api/validate/trust-lists")
public class TrustListResource {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Response status() {
		PdfValidationService service = AppContext.getValidationService();
		TrustListStatusResponse status = service.getTrustListStatus();
		return Response.ok(status).build();
	}

	@POST
	@Path("/refresh")
	@Produces(MediaType.APPLICATION_JSON)
	public Response refresh() {
		PdfValidationService service = AppContext.getValidationService();
		service.refreshTrustListsAsync();
		TrustListStatusResponse status = service.getTrustListStatus();
		return Response.ok(status).build();
	}

	@GET
	@Path("/stream")
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void stream(@Context SseEventSink eventSink, @Context Sse sse) {
		if (eventSink == null) {
			return;
		}
		PdfValidationService service = AppContext.getValidationService();
		Thread thread = new Thread(() -> {
			String lastPayload = null;
			int idleTicks = 0;
			try {
				while (!eventSink.isClosed()) {
					TrustListStatusResponse status = service.getTrustListStatus();
					String json = MAPPER.writeValueAsString(status);
					if (!json.equals(lastPayload)) {
						lastPayload = json;
						eventSink.send(sse.newEventBuilder()
								.name("status")
								.data(String.class, json)
								.build());
						idleTicks = 0;
					} else {
						idleTicks += 1;
						if (idleTicks >= 10) {
							eventSink.send(sse.newEventBuilder()
									.name("ping")
									.data(String.class, "ping")
									.build());
							idleTicks = 0;
						}
					}
					Thread.sleep(1000);
				}
			} catch (Exception ignored) {
				// Client likely disconnected or stream closed.
			} finally {
				try {
					eventSink.close();
				} catch (Exception ignored) {
					// noop
				}
			}
		}, "dss-trustlist-sse");
		thread.setDaemon(true);
		thread.start();
	}
}
