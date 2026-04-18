package com.smartcampus.exception.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Unprocessable Entity");
        response.put("message", exception.getMessage());
        return Response.status(422) // 422 Unprocessable Entity
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
