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
import java.util.Map;
import io.mastermindarena.deduction.api.submitaction.HttpObservability.RuntimeMetrics;
import io.mastermindarena.deduction.api.submitaction.HttpObservability.RuntimeMetricsSnapshot;
import static io.mastermindarena.deduction.api.submitaction.HttpObservability.logStructured;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.elapsedMillis;

/** Package-local HTTP adapter; business rules remain in the application service. */
final class GameHttpHandlers {
    private GameHttpHandlers() {}

    static void handleSubmitAction(
            HttpExchange exchange,
            LocalSubmitActionEndpoint endpoint,
            JdbcRoomDirectory rooms,
            ObjectMapper objectMapper,
            String apiKeyHeaderName,
            String apiKeyValue,
            String requestIdHeaderName,
            CorsPolicy corsPolicy,
            RuntimeMetrics metrics
    ) throws IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(exchange, requestIdHeaderName);
        setResponseRequestIdHeader(exchange, requestIdHeaderName, requestId);
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!applyCors(exchange, corsPolicy)) {
                logStructured("request_rejected", path, method, 403, "CORS_ORIGIN_NOT_ALLOWED", requestId, elapsedMillis(startNanos), metrics.record(403, elapsedMillis(startNanos)));
                writeJson(exchange, objectMapper, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED"));
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!LocalSubmitActionEndpoint.METHOD.equalsIgnoreCase(exchange.getRequestMethod())) {
                logStructured("request_rejected", path, method, 405, "METHOD_NOT_ALLOWED", requestId, elapsedMillis(startNanos), metrics.record(405, elapsedMillis(startNanos)));
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            String providedApiKey = exchange.getRequestHeaders().getFirst(apiKeyHeaderName);
            if (!secureEquals(apiKeyValue, providedApiKey)) {
                logStructured("request_rejected", path, method, 401, "UNAUTHORIZED", requestId, elapsedMillis(startNanos), metrics.record(401, elapsedMillis(startNanos)));
                writeJson(exchange, objectMapper, 401, new ErrorBody("UNAUTHORIZED"));
                return;
            }

            SubmitActionHttpRequest request = readBody(exchange, objectMapper, SubmitActionHttpRequest.class);
            if (!rooms.authorize(request.matchId(), request.actorId(), exchange.getRequestHeaders().getFirst("X-Room-Token"))) {
                writeJson(exchange, objectMapper, 403, new ErrorBody("ROOM_ACCESS_DENIED"));
                return;
            }
            if (rooms.waitingForGuest(request.matchId())) {
                writeJson(exchange, objectMapper, 409, new ErrorBody("ROOM_WAITING_FOR_PLAYER"));
                return;
            }
            SubmitActionHttpResponse.Envelope response = endpoint.postSubmitAction(request);
            long elapsedMillis = elapsedMillis(startNanos);
            logStructured(
                    "submit_action",
                    path,
                    method,
                    response.statusCode(),
                    response.body().accepted() ? "ACCEPTED" : response.body().rejectionCode(),
                    requestId,
                    elapsedMillis,
                    metrics.record(response.statusCode(), elapsedMillis)
            );
            writeJson(exchange, objectMapper, response.statusCode(), response.body());
        } catch (RuntimeException ex) {
            writeRequestFailure(exchange, objectMapper, ex, startNanos, requestId, metrics);
        } finally {
            exchange.close();
        }
    }

    static void handleHealth(
            HttpExchange exchange,
            ObjectMapper objectMapper,
            String requestIdHeaderName,
            CorsPolicy corsPolicy,
            RuntimeMetrics metrics
    ) throws IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(exchange, requestIdHeaderName);
        setResponseRequestIdHeader(exchange, requestIdHeaderName, requestId);
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!applyCors(exchange, corsPolicy)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 403, "CORS_ORIGIN_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(403, elapsedMillis));
                writeJson(exchange, objectMapper, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED"));
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 405, "METHOD_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(405, elapsedMillis));
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            long elapsedMillis = elapsedMillis(startNanos);
            RuntimeMetricsSnapshot snapshot = metrics.record(200, elapsedMillis);
            logStructured("health", path, method, 200, "UP", requestId, elapsedMillis, snapshot);
            writeJson(exchange, objectMapper, 200, new HealthBody("UP", snapshot.requestsOk(), snapshot.requestsKo(), snapshot.averageLatencyMs()));
        } catch (RuntimeException ex) {
            writeRequestFailure(exchange, objectMapper, ex, startNanos, requestId, metrics);
        } finally {
            exchange.close();
        }
    }

    static void handleMatchState(
            HttpExchange exchange,
            LocalMatchStateEndpoint endpoint,
            JdbcRoomDirectory rooms,
            ObjectMapper objectMapper,
            String apiKeyHeaderName,
            String apiKeyValue,
            String requestIdHeaderName,
            CorsPolicy corsPolicy,
            RuntimeMetrics metrics
    ) throws IOException {
        long startNanos = System.nanoTime();
        String requestId = resolveRequestId(exchange, requestIdHeaderName);
        setResponseRequestIdHeader(exchange, requestIdHeaderName, requestId);
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (!applyCors(exchange, corsPolicy)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 403, "CORS_ORIGIN_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(403, elapsedMillis));
                writeJson(exchange, objectMapper, 403, new ErrorBody("CORS_ORIGIN_NOT_ALLOWED"));
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!LocalMatchStateEndpoint.METHOD.equalsIgnoreCase(method)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 405, "METHOD_NOT_ALLOWED", requestId, elapsedMillis, metrics.record(405, elapsedMillis));
                writeJson(exchange, objectMapper, 405, new ErrorBody("METHOD_NOT_ALLOWED"));
                return;
            }

            String providedApiKey = exchange.getRequestHeaders().getFirst(apiKeyHeaderName);
            if (!secureEquals(apiKeyValue, providedApiKey)) {
                long elapsedMillis = elapsedMillis(startNanos);
                logStructured("request_rejected", path, method, 401, "UNAUTHORIZED", requestId, elapsedMillis, metrics.record(401, elapsedMillis));
                writeJson(exchange, objectMapper, 401, new ErrorBody("UNAUTHORIZED"));
                return;
            }

            Map<String, String> queryParams = queryParams(exchange.getRequestURI().getRawQuery());
            String matchId = queryParams.get("matchId");
            String actorId = queryParams.get("actorId");
            if (!rooms.authorize(matchId, actorId, exchange.getRequestHeaders().getFirst("X-Room-Token"))) {
                writeJson(exchange, objectMapper, 403, new ErrorBody("ROOM_ACCESS_DENIED"));
                return;
            }
            MatchStateHttpResponse.Envelope response = endpoint.getMatchState(matchId, actorId);
            long elapsedMillis = elapsedMillis(startNanos);

            if (response.statusCode() == 200) {
                logStructured("match_state", path, method, 200, "FOUND", requestId, elapsedMillis, metrics.record(200, elapsedMillis));
                writeJson(exchange, objectMapper, 200, response.body());
                return;
            }

            if (response.statusCode() == 404) {
                logStructured("request_rejected", path, method, 404, "MATCH_NOT_FOUND", requestId, elapsedMillis, metrics.record(404, elapsedMillis));
                writeJson(exchange, objectMapper, 404, new ErrorBody("MATCH_NOT_FOUND"));
                return;
            }

            logStructured("request_rejected", path, method, 400, "INVALID_MATCH_ID", requestId, elapsedMillis, metrics.record(400, elapsedMillis));
            writeJson(exchange, objectMapper, 400, new ErrorBody("INVALID_MATCH_ID"));
        } catch (RuntimeException ex) {
            writeRequestFailure(exchange, objectMapper, ex, startNanos, requestId, metrics);
        } finally {
            exchange.close();
        }
    }

    private static void writeRequestFailure(HttpExchange exchange, ObjectMapper objectMapper,
            RuntimeException ex, long startNanos, String requestId, RuntimeMetrics metrics) throws IOException {
            long elapsedMillis = elapsedMillis(startNanos);
            int errorStatus = ex instanceof IllegalArgumentException ? 400 : 500;
            String errorCode = ex instanceof IllegalArgumentException ? "INVALID_REQUEST" : "SERVER_ERROR";
            logStructured(
                    "request_rejected",
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestMethod(),
                    errorStatus,
                    errorCode,
                    requestId,
                    elapsedMillis,
                    metrics.record(errorStatus, elapsedMillis)
            );
            writeJson(exchange, objectMapper, errorStatus, new ErrorBody(errorCode));
    }

    record HealthBody(
            String status,
            long requestsOk,
            long requestsKo,
            long averageLatencyMs
    ) {
    }
}
