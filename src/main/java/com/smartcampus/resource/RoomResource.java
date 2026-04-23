package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Resource class for managing Rooms in the Smart Campus system.
 * Provides endpoints for creating, retrieving, and deleting rooms.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private DataStore dataStore = DataStore.getInstance();

    /**
     * Retrieves all rooms currently stored in the system.
     * @return 200 OK with a list of all Room objects.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = dataStore.getRooms().values();
        return Response.ok(rooms).build();
    }

    /**
     * Creates a new room. If an ID is not provided, a random UUID will be generated.
     * @param room The Room object to create.
     * @param uriInfo Context for building the location URI of the new resource.
     * @return 201 Created with the location of the new room and the room object itself.
     */
    @POST
    public Response createRoom(Room room, @jakarta.ws.rs.core.Context jakarta.ws.rs.core.UriInfo uriInfo) {
        if (room.getId() != null && dataStore.getRooms().containsKey(room.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Conflict");
            error.put("message", "Room with ID " + room.getId() + " already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        dataStore.getRooms().put(room.getId(), room);
        java.net.URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(room).build();
    }

    /**
     * Deletes a specific room by its ID.
     * Business Logic: A room cannot be deleted if it still has sensors assigned to it.
     * @param roomId The unique identifier of the room to delete.
     * @return 204 No Content if successful, 404 Not Found if the room doesn't exist, 
     *         or 409 Conflict if the room has active sensors.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room " + roomId + " because it still has active sensors assigned to it.");
        }
        
        dataStore.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}
