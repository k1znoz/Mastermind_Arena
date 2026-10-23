package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.mastermindarena.deduction.infrastructure.jdbc.JdbcRoomDirectory;
import java.io.IOException;
import io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.CorsPolicy;
import io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.ErrorBody;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.applyCors;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.readBody;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.resolveRequestId;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.secureEquals;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.setResponseRequestIdHeader;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.writeJson;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.queryParams;
import java.util.List;
import static io.mastermindarena.deduction.api.submitaction.HttpObservability.logRoomPersistenceFailure;

/** Package-local HTTP adapter; business rules remain in the application service. */
final class RoomHttpHandlers {
    private RoomHttpHandlers() {}

    record CreateRoomBody(String name, String pseudo, String accessCode) {}

    record JoinRoomBody(String roomId, String pseudo, String accessCode) {}

    record ResumeRoomBody(String recoveryCode) {}

    record RoomListBody(List<JdbcRoomDirectory.RoomInfo> rooms) {}

    static void handleRooms(
            HttpExchange exchange, JdbcRoomDirectory rooms, ObjectMapper json,
            String apiHeader, String apiKey, String requestHeader, CorsPolicy cors
    ) throws IOException {
        String requestId = resolveRequestId(exchange, requestHeader);
        setResponseRequestIdHeader(exchange, requestHeader, requestId);
        try {
            if (!applyCors(exchange, cors)) { writeJson(exchange, json, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED")); return; }
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!secureEquals(apiKey, exchange.getRequestHeaders().getFirst(apiHeader))) {
                writeJson(exchange, json, 401, new ErrorBody("UNAUTHORIZED")); return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String roomId = queryParams(exchange.getRequestURI().getRawQuery()).get("roomId");
                    if (roomId == null || roomId.isBlank()) {
                        writeJson(exchange, json, 200, new RoomListBody(rooms.waitingRooms()));
                    } else {
                        var info = rooms.findInfo(roomId);
                        writeJson(exchange, json, info.isPresent() ? 200 : 404,
                                info.<Object>map(value -> value).orElse(new ErrorBody("ROOM_NOT_FOUND")));
                    }
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    CreateRoomBody body = readBody(exchange, json, CreateRoomBody.class);
                    writeJson(exchange, json, 201, rooms.create(body.name(), body.pseudo(), body.accessCode()));
                } else {
                    writeJson(exchange, json, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                }
            } catch (IllegalStateException e) {
                writeRoomError(exchange, json, e, requestId);
            } catch (IllegalArgumentException e) {
                writeJson(exchange, json, 400, new ErrorBody("INVALID_REQUEST"));
            } catch (RuntimeException failure) {
                writeUnexpectedRoomError(exchange, json, failure, requestId);
            }
        } finally {
            exchange.close();
        }
    }

    static void handleRoomJoin(
            HttpExchange exchange, JdbcRoomDirectory rooms, ObjectMapper json,
            String apiHeader, String apiKey, String requestHeader, CorsPolicy cors
    ) throws IOException {
        String requestId = resolveRequestId(exchange, requestHeader);
        setResponseRequestIdHeader(exchange, requestHeader, requestId);
        try {
            if (!applyCors(exchange, cors)) { writeJson(exchange, json, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED")); return; }
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, json, 405, new ErrorBody("METHOD_NOT_ALLOWED")); return;
            }
            if (!secureEquals(apiKey, exchange.getRequestHeaders().getFirst(apiHeader))) {
                writeJson(exchange, json, 401, new ErrorBody("UNAUTHORIZED")); return;
            }
            try {
                JoinRoomBody body = readBody(exchange, json, JoinRoomBody.class);
                writeJson(exchange, json, 200, rooms.join(body.roomId(), body.pseudo(), body.accessCode()));
            } catch (IllegalStateException e) {
                writeRoomError(exchange, json, e, requestId);
            } catch (IllegalArgumentException e) {
                writeJson(exchange, json, 400, new ErrorBody("INVALID_REQUEST"));
            } catch (RuntimeException failure) {
                writeUnexpectedRoomError(exchange, json, failure, requestId);
            }
        } finally {
            exchange.close();
        }
    }

    static void writeRoomError(
            HttpExchange exchange,
            ObjectMapper json,
            IllegalStateException e,
            String requestId
    ) throws IOException {
        String code = java.util.Objects.toString(e.getMessage(), "ROOM_SERVER_ERROR");
        int status = switch (code) {
            case "ROOM_NOT_FOUND" -> 404;
            case "ROOM_FULL", "ROOM_WAITING_FOR_PLAYER" -> 409;
            case "ROOM_CODE_INVALID", "ROOM_RESUME_INVALID" -> 403;
            case "ROOM_NAME_REQUIRED", "PSEUDO_REQUIRED", "FIELD_TOO_LONG" -> 400;
            default -> 500;
        };
        if (status == 500) {
            logRoomPersistenceFailure("ROOM_SERVER_ERROR", requestId, e);
        }
        writeJson(exchange, json, status, new ErrorBody(status == 500 ? "ROOM_SERVER_ERROR" : code));
    }

    static void handleRoomResume(HttpExchange exchange, JdbcRoomDirectory rooms, ObjectMapper json,
            String apiHeader, String apiKey, String requestHeader, CorsPolicy cors) throws IOException {
        String requestId = resolveRequestId(exchange, requestHeader);
        setResponseRequestIdHeader(exchange, requestHeader, requestId);
        try {
            if (!applyCors(exchange, cors)) { writeJson(exchange, json, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED")); return; }
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { writeJson(exchange, json, 405, new ErrorBody("METHOD_NOT_ALLOWED")); return; }
            if (!secureEquals(apiKey, exchange.getRequestHeaders().getFirst(apiHeader))) {
                writeJson(exchange, json, 401, new ErrorBody("UNAUTHORIZED")); return;
            }
            try {
                ResumeRoomBody body = readBody(exchange, json, ResumeRoomBody.class);
                writeJson(exchange, json, 200, rooms.resume(body.recoveryCode()));
            } catch (IllegalStateException failure) {
                writeRoomError(exchange, json, failure, requestId);
            } catch (IllegalArgumentException failure) {
                writeJson(exchange, json, 400, new ErrorBody("INVALID_REQUEST"));
            } catch (RuntimeException failure) {
                writeUnexpectedRoomError(exchange, json, failure, requestId);
            }
        } finally {
            exchange.close();
        }
    }

    private static void writeUnexpectedRoomError(HttpExchange exchange, ObjectMapper json,
            RuntimeException failure, String requestId) throws IOException {
        logRoomPersistenceFailure("ROOM_SERVER_ERROR", requestId, failure);
        writeJson(exchange, json, 500, new ErrorBody("ROOM_SERVER_ERROR"));
    }
}
