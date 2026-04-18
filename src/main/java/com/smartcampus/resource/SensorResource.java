package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore dataStore = DataStore.getInstance();

    @POST
    public Response createSensor(Sensor sensor) {
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

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

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

    // Sub-Resource Locator Pattern
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
