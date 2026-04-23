package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resource class for managing Sensors in the Smart Campus system.
 * Handles sensor registration, retrieval, and sub-resource delegation for readings.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore dataStore = DataStore.getInstance();

    /**
     * Registers a new sensor and links it to a room.
     * @param sensor The Sensor object to create. Must include a valid roomId.
     * @param uriInfo Context for building the location URI.
     * @return 201 Created if successful, or 422 Unprocessable Entity if roomId is invalid.
     */
    @POST
    public Response createSensor(Sensor sensor, @jakarta.ws.rs.core.Context jakarta.ws.rs.core.UriInfo uriInfo) {
        if (sensor.getId() != null && dataStore.getSensors().containsKey(sensor.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Conflict");
            error.put("message", "Sensor with ID " + sensor.getId() + " already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        String roomId = sensor.getRoomId();
        if (roomId == null || !dataStore.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException("Room with ID " + roomId + " does not exist.");
        }

        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        dataStore.getSensors().put(sensor.getId(), sensor);

        // Add sensor to the assigned room
        Room room = dataStore.getRooms().get(roomId);
        room.getSensorIds().add(sensor.getId());

        java.net.URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    /**
     * Retrieves a specific sensor by its ID.
     * @param sensorId The unique identifier of the sensor.
     * @return 200 OK with the Sensor object, or 404 Not Found.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * Retrieves sensors, optionally filtered by type.
     * @param type (Optional) The type of sensor to filter by (e.g., "Temperature").
     * @return 200 OK with the list of sensors.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> allSensors = dataStore.getSensors().values();
        
        if (type != null && !type.isEmpty()) {
            List<Sensor> filteredSensors = new ArrayList<>();
            for (Sensor sensor : allSensors) {
                if (type.equalsIgnoreCase(sensor.getType())) {
                    filteredSensors.add(sensor);
                }
            }
            return Response.ok(filteredSensors).build();
        }

        return Response.ok(allSensors).build();
    }

    /**
     * Sub-resource locator for sensor readings.
     * Delegates requests for /sensors/{id}/readings to SensorReadingResource.
     * @param sensorId The unique identifier of the sensor.
     * @return A new instance of SensorReadingResource.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        if (!dataStore.getSensors().containsKey(sensorId)) {
            throw new ResourceNotFoundException("Sensor with ID " + sensorId + " does not exist.");
        }
        return new SensorReadingResource(sensorId);
    }
}
