package com.smartcampus.config;

import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
public class RestApplication extends ResourceConfig {
    public RestApplication() {
        // Registering packages where Jersey will look for resource classes, exception mappers, etc.
        packages("com.smartcampus.resource", "com.smartcampus.exception.mapper", "com.smartcampus.filter");
    }
}
