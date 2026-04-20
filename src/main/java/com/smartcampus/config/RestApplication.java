package com.smartcampus.config;

import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.ApplicationPath;

import com.smartcampus.resource.*;
import com.smartcampus.exception.mapper.*;
import com.smartcampus.filter.LoggingFilter;

@ApplicationPath("/api/v1")
public class RestApplication extends ResourceConfig {
    public RestApplication() {
        packages("com.smartcampus.resource", "com.smartcampus.exception.mapper", "com.smartcampus.filter");
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);        
        // Do not register SensorReadingResource directly as it is a sub-resource locator spawned by SensorResource
        
        register(GlobalExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(RoomNotEmptyExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(LoggingFilter.class);
    }
}
