package com.smartcampus.resource;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for managing readings of a specific sensor.
 * Handles adding new readings and retrieving the history of readings.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;
    private DataStore dataStore = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * Retrieves all readings for the associated sensor.
     * 
     * @return 200 OK with the list of readings.
     */
    @GET
    public Response getReadings() {
        if (!dataStore.getSensors().containsKey(sensorId)) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " does not exist.");
        }

        List<SensorReading> readings = dataStore.getSensorReadings().getOrDefault(sensorId,
                new CopyOnWriteArrayList<>());
        return Response.ok(readings).build();
    }

    /**
     * Adds a new reading for the sensor.
     * Business Logic:
     * 1. Updates the sensor's currentValue.
     * 2. Prevents readings if sensor status is "MAINTENANCE".
     * 
     * @param reading The SensorReading object to add.
     * @return 201 Created if successful, 403 Forbidden if sensor is unavailable.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " does not exist.");
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()) || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor " + sensorId + " is currently " + sensor.getStatus() + " and cannot accept readings.");
        }

        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Side effect: update sensor's current value
        sensor.setCurrentValue(reading.getValue());

        dataStore.getSensorReadings().putIfAbsent(sensorId, new CopyOnWriteArrayList<>());
        dataStore.getSensorReadings().get(sensorId).add(reading);

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
