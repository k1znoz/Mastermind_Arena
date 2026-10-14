package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;
import io.mastermindarena.deduction.engine.workflow.InMemoryEventSink;
import io.mastermindarena.deduction.engine.workflow.InMemoryMatchStateStore;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionOrchestrator;
import io.mastermindarena.deduction.infrastructure.jdbc.RoomCredentials;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/** Local UI validation adapter. Uses the real application/workflow, but NO JDBC or production data. */
public final class UiValidationServer {
    private UiValidationServer() {}

    private static final class Room {
        private final String id = UUID.randomUUID().toString();
        private final String hostToken = RoomCredentials.newToken();
        private final String guestToken = RoomCredentials.newToken();
        private final String name;
        private final String hostPseudo;
        private final String code;
        private String guestPseudo;
        private Room(String name, String pseudo, String code) { this.name = name; this.hostPseudo = pseudo; this.code = code; }
        private Map<String, Object> info() {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("roomId", id); info.put("name", name); info.put("hostPseudo", hostPseudo);
            info.put("guestPseudo", guestPseudo); info.put("locked", !code.isBlank());
            info.put("createdAt", 0); info.put("joined", guestPseudo != null);
            return info;
        }
        private Map<String, Object> admission(String actor) {
            return Map.of("room", info(), "actorId", actor, "token", "p1".equals(actor) ? hostToken : guestToken);
        }
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper json = new ObjectMapper();
        InMemoryMatchStateStore states = new InMemoryMatchStateStore();
        LocalSubmitActionEndpoint actions = new LocalSubmitActionEndpoint(new SubmitActionApplicationService(
                new SubmitActionOrchestrator(states, new InMemoryEventSink())));
        Map<String, Room> rooms = new LinkedHashMap<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8099), 0);
        server.createContext("/local", exchange -> {
            try (exchange) {
                Object response;
                int status = 200;
                try {
                    String path = exchange.getRequestURI().getPath();
                    if (path.startsWith("/local/rooms")) {
                        if ("GET".equals(exchange.getRequestMethod())) {
                            String query = exchange.getRequestURI().getRawQuery();
                            response = query == null ? Map.of("rooms", rooms.values().stream().filter(room -> room.guestPseudo == null).map(Room::info).toList())
                                    : rooms.get(query.substring("roomId=".length())).info();
                        } else {
                            var body = json.readTree(exchange.getRequestBody());
                            if (path.endsWith("/resume")) {
                                String[] parts = body.path("recoveryCode").asText().split("\\.");
                                Room room = parts.length == 3 ? rooms.get(parts[0]) : null;
                                if (room == null || !("p1".equals(parts[1]) ? room.hostToken : room.guestToken).equals(parts[2])) throw new IllegalArgumentException("ROOM_RESUME_INVALID");
                                response = room.admission(parts[1]);
                            } else if (path.endsWith("/join")) {
                                Room room = rooms.get(body.path("roomId").asText());
                                if (room == null) throw new IllegalArgumentException("ROOM_NOT_FOUND");
                                if (room.guestPseudo != null) throw new IllegalArgumentException("ROOM_FULL");
                                if (!room.code.equals(body.path("accessCode").asText())) throw new IllegalArgumentException("ROOM_CODE_INVALID");
                                room.guestPseudo = body.path("pseudo").asText();
                                response = room.admission("p2");
                            } else {
                                Room room = new Room(body.path("name").asText(), body.path("pseudo").asText(), body.path("accessCode").asText());
                                rooms.put(room.id, room);
                                states.save(new MatchRuntimeState(room.id, List.of("p1", "p2")));
                                response = room.admission("p1");
                            }
                        }
                    } else if (path.endsWith("/submit-action")) {
                        SubmitActionHttpRequest request = json.readValue(exchange.getRequestBody(), SubmitActionHttpRequest.class);
                        Room room = rooms.get(request.matchId());
                        requireToken(room, request.actorId(), exchange.getRequestHeaders().getFirst("X-Room-Token"));
                        var result = actions.postSubmitAction(request);
                        status = result.statusCode(); response = result.body();
                    } else {
                        Map<String, String> params = new LinkedHashMap<>();
                        for (String part : exchange.getRequestURI().getRawQuery().split("&")) {
                            String[] pair = part.split("=", 2); params.put(pair[0], pair[1]);
                        }
                        String match = params.get("matchId"), actor = params.get("actorId");
                        requireToken(rooms.get(match), actor, exchange.getRequestHeaders().getFirst("X-Room-Token"));
                        response = MatchStateHttpMapper.fromRuntimeState(states.findById(match).orElseThrow(), actor);
                    }
                } catch (RuntimeException failure) {
                    status = 400; response = Map.of("code", failure.getMessage() == null ? "INVALID_REQUEST" : failure.getMessage());
                }
                byte[] bytes = json.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        System.out.println("Local UI adapter listening on 127.0.0.1:8099 (memory only, no PostgreSQL)");
        new CountDownLatch(1).await();
    }

    private static void requireToken(Room room, String actor, String token) {
        if (room == null || !("p1".equals(actor) ? room.hostToken : "p2".equals(actor) ? room.guestToken : "").equals(token)) {
            throw new IllegalArgumentException("ROOM_ACCESS_DENIED");
        }
    }
}
