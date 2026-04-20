package com.smartcampus.exception.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof jakarta.ws.rs.WebApplicationException) {
            Response originalResponse = ((jakarta.ws.rs.WebApplicationException) exception).getResponse();
            if (!originalResponse.hasEntity()) {
                Map<String, String> body = new HashMap<>();
                body.put("error", originalResponse.getStatusInfo().getReasonPhrase());
                body.put("message", exception.getMessage() != null ? exception.getMessage() : "HTTP constraint or definition error");
                return Response.fromResponse(originalResponse)
                        .entity(body)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return originalResponse;
        }

        // Log the actual exception trace securely on the server side
        exception.printStackTrace();

        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred. Please contact an administrator.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
