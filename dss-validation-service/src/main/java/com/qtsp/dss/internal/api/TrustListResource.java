package com.qtsp.dss.internal.api;

import com.qtsp.dss.internal.app.AppContext;
import com.qtsp.dss.internal.model.TrustListStatusResponse;
import com.qtsp.dss.internal.validation.PdfValidationService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/validate/trust-lists")
public class TrustListResource {
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
}
