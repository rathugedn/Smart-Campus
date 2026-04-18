# Smart Campus Sensor & Room Management API

A robust, highly available RESTful API to manage Rooms and Sensors using JAX-RS (Jakarta RESTful Web Services), serving as the endpoint hub for a campus-wide infrastructure project.

Built strictly with Jersey, Grizzly Server, Jackson for JSON parsing, without using standard DB systems (utilizing thread-safe in-memory collections natively).

## Build and Launch Instructions

### Prerequisites
- JDK 11 or higher
- Maven 3.6+

### Steps
1. **Navigate to the root** of the repository:
   ```bash
   cd Smart-Campus
   ```
2. **Compile the project**:
   ```bash
   mvn clean compile
   ```
3. **Run the server**:
   ```bash
   mvn exec:java
   ```
   The Grizzly server will start and listen on `http://localhost:8081/api/v1`.

## Sample API Interactions (cURL)

**1. Discovery Metadata**
```bash
curl -X GET http://localhost:8081/api/v1
```

**2. Create a generic Room**
```bash
curl -X POST http://localhost:8081/api/v1/rooms \
    -H "Content-Type: application/json" \
    -d '{"id":"CS-101", "name":"CS Lab", "capacity":30}'
```

**3. Get all Rooms**
```bash
curl -X GET http://localhost:8081/api/v1/rooms
```

**4. Register a new Sensor (Linked to Room CS-101)**
```bash
curl -X POST http://localhost:8081/api/v1/sensors \
    -H "Content-Type: application/json" \
    -d '{"type":"CO2", "status":"ACTIVE", "currentValue":400.0, "roomId":"CS-101"}'
```

**5. Get Sensors containing filter**
```bash
curl -X GET 'http://localhost:8081/api/v1/sensors?type=CO2'
```

**6. Submit a Sensor Reading**
```bash
# Substitute {sensorId} below with the ID generated from step 4
curl -X POST http://localhost:8081/api/v1/sensors/{sensorId}/readings \
    -H "Content-Type: application/json" \
    -d '{"value":415.5}'
```

**7. Error Generation - 409 Conflict**
```bash
# Deleting a room with active registered sensors throws 409.
curl -v -X DELETE http://localhost:8081/api/v1/rooms/CS-101
```

---

## Conceptual Report & Answers

### Part 1: Service Architecture & Setup
**Q: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.**
**A:** By default, JAX-RS treats Resource classes as "per-request," meaning a new instance is constructed for each incoming HTTP request. Because numerous instances could be interacting with the storage layer simultaneously, any static or shared in-memory data structures must be thread-safe. To securely avoid data loss or race conditions, we utilize standard thread-safe collections like `ConcurrentHashMap` inside a centralized Singleton `DataStore`.

**Q: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**
**A:** HATEOAS allows clients to dynamically discover what actions are available and where resources reside strictly via the API responses, loosely coupling client paths from server endpoints. It reduces the dependency on external static documentation and allows servers to restructure URL schemas without breaking external clients, simply by updating the URIs exposed dynamically in the Hypermedia links within payloads.

### Part 2: Room Management
**Q: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.**
**A:** Returning only IDs reduces network bandwidth significantly and speeds up HTTP transmission, but at the cost of forcing clients to issue numerous subsequent `GET /rooms/{id}` calls if they wish to render meaningful data properties (the N+1 fetch problem). On the contrary, returning complete objects expends more initial network bandwidth but facilitates immediate client-side rendering with a single request. 

**Q: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.**
**A:** Yes, the DELETE endpoint operation is idempotent. If a client mistakenly sends the exact same `DELETE` request for a valid Room (with zero active sensors), the first request destroys the resource. Upon receiving the subsequent duplicate DELETE request, the Room will no longer exist in the datastore, and the server will gracefully return a deterministic `404 NOT FOUND`. The data state of the application remains unchanged regardless of whether 1 or 1,000 extra sequential deletes are fired.

### Part 3: Sensor Operations & Linking
**Q: We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?**
**A:** Because of this strict binding, if a client tries to deliver a payload outside of that media type format, JAX-RS' container will immediately block the request before it even reaches the Java method logic. JAX-RS handles this by abruptly failing the transaction natively and returning a `415 Unsupported Media Type` HTTP status.

**Q: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?**
**A:** Query parameters do not change the fundamental "identity" of where the Resource resides but simply act as modifiers affecting the window of fetched data. The URI path denotes hierarchical identity (e.g., this exact Sensor), while query semantics are reserved for optional sorting, pagination, and multi-variable filtering (because their positional hierarchy does not strongly matter compared to structural path parameters).

### Part 4: Deep Nesting with Sub-Resources
**Q: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?**
**A:** Using a Sub-Resource Locator promotes single-responsibility architecture and keeps individual class definitions small, readable, and highly cohesive. By delegating all logic regarding "/readings" exclusively to `SensorReadingResource`, the API intuitively decouples parent logic (managing static Sensor entities) from child logic (interpreting time-series logs). In massive APIs, doing all mapping inside one super-controller makes dependency injection heavy and debugging highly convoluted.

### Part 5: Advanced Error Handling, Exception Mapping & Logging
**Q: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**
**A:** A standard `404 Not Found` inherently means the routing path hit natively does not exist (the client typed the wrong URL endpoint address). An `422 Unprocessable Entity` correctly implies the endpoint was reached effectively, and the HTTP JSON was properly parsed, but the underlying business restrictions within the payload logic (referencing a `roomId` that doesn't correspond to any entity in the datastore) prevented the server from proceeding.

**Q: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**
**A:** A raw server stack trace exposes highly granular structural data about the internal backend deployment, including database query structures, internal physical system paths, framework versions utilized, active third-party dependencies, and precise variable nomenclature. An attacker can use these specific framework identifiers or class trace details to cross-reference and exploit known zero-day vulnerabilities specifically correlated directly to those package configurations.

**Q: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?**
**A:** Cross-cutting implementation via JAX-RS filter allows observing logic strictly isolated natively across the entire App via inversion of control, bypassing heavy amounts of boilerplate. This guarantees uniform observability standards because newly developed methods or paths will automatically integrate logging properties without manual coding dependencies.
