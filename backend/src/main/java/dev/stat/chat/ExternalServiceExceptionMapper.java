package dev.stat.chat;

import dev.stat.chat.dto.ErrorResponse;
import dev.stat.chat.service.ExternalServiceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps ExternalServiceException to HTTP 503 Service Unavailable.
 * Per spec: if ANY external service fails, return 503 with error details.
 */
@Provider
public class ExternalServiceExceptionMapper implements ExceptionMapper<ExternalServiceException> {

    @Override
    public Response toResponse(ExternalServiceException exception) {
        var error = new ErrorResponse("EXTERNAL_API_FAILURE", exception.getMessage());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(error)
                .build();
    }
}
