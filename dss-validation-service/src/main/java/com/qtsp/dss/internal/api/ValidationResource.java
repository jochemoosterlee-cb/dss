package com.qtsp.dss.internal.api;

import com.qtsp.dss.internal.app.AppContext;
import com.qtsp.dss.internal.app.ServiceConfig;
import com.qtsp.dss.internal.model.ValidationSummaryResponse;
import com.qtsp.dss.internal.validation.PdfValidationService;
import com.qtsp.dss.internal.validation.ValidationException;
import com.qtsp.dss.internal.validation.ValidationRequestOptions;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Path("/api/validate")
public class ValidationResource {
	@POST
	@Path("/pdf")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response validatePdf(
			@FormDataParam("file") InputStream fileStream,
			@FormDataParam("file") FormDataContentDisposition fileMeta,
			@FormDataParam("mode") String mode,
			@FormDataParam("policy") String policy,
			@FormDataParam("requireRevocation") String requireRevocation,
			@FormDataParam("trustList") String trustList) {
		try {
			if (fileStream == null) {
				return badRequest("Missing 'file' part");
			}

			ServiceConfig config = AppContext.getConfig();
			byte[] bytes;
			try (InputStream input = fileStream) {
				bytes = readBytes(input, config.getMaxUploadBytes());
			}
			String filename = fileMeta != null ? fileMeta.getFileName() : null;

			ValidationRequestOptions options = ValidationRequestOptions.from(mode, policy, requireRevocation, trustList);
			PdfValidationService validationService = AppContext.getValidationService();
			ValidationSummaryResponse response = validationService.validate(bytes, filename, options);
			return Response.ok(response).build();
		} catch (ValidationException e) {
			ValidationSummaryResponse response = ValidationSummaryResponse.indeterminateWithError(e.getMessage());
			return Response.status(e.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(response).build();
		} catch (Exception e) {
			ValidationSummaryResponse response = ValidationSummaryResponse.indeterminateWithError("Internal error");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(response).build();
		} catch (Throwable t) {
			String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
			ValidationSummaryResponse response = ValidationSummaryResponse.indeterminateWithError(message);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(response).build();
		}
	}

	private Response badRequest(String message) {
		ValidationSummaryResponse response = ValidationSummaryResponse.indeterminateWithError(message);
		return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(response).build();
	}

	private byte[] readBytes(InputStream inputStream, int maxBytes) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int total = 0;
		int read;
		while ((read = inputStream.read(buffer)) != -1) {
			total += read;
			if (total > maxBytes) {
				throw new ValidationException(413, "File too large");
			}
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}
}
