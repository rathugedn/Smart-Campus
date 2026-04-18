package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorUnavailableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Forbidden");
        response.put("message", exception.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
