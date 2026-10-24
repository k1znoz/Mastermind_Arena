package io.mastermindarena.deduction.api.submitaction;

import java.sql.SQLException;
import com.sun.net.httpserver.HttpHandler;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import static io.mastermindarena.deduction.api.submitaction.HttpExchangeSupport.escapeJson;

/** Structured request diagnostics and in-process counters, without request payloads. */
final class HttpObservability {
    private HttpObservability() {}

    /** Count lobby responses once, including refusals and preflights. No body/query/credential values logged. */
    static HttpHandler observeRoom(HttpHandler handler, RuntimeMetrics metrics, String requestHeader) {
        return exchange -> {
            long start = System.nanoTime();
            try {
                handler.handle(exchange);
            } finally {
                int status = Math.max(0, exchange.getResponseCode());
                long duration = HttpExchangeSupport.elapsedMillis(start);
                String requestId = exchange.getResponseHeaders().getFirst(requestHeader);
                logStructured("room_request", exchange.getHttpContext().getPath(), exchange.getRequestMethod(),
                        status, status == 0 ? "NO_RESPONSE" : "HTTP_" + status,
                        requestId == null ? "n/a" : requestId, duration, metrics.record(status, duration));
            }
        };
    }

    static void logStructured(
            String event,
            String path,
            String method,
            int statusCode,
            String outcome,
            String requestId,
            long durationMs,
            RuntimeMetricsSnapshot metrics
    ) {
        String escapedEvent = escapeJson(event);
        String escapedPath = escapeJson(path);
        String escapedMethod = escapeJson(method);
        String escapedOutcome = escapeJson(outcome);
        String escapedRequestId = escapeJson(requestId);
        System.out.println(String.format(Locale.ROOT,
                "{\"event\":\"%s\",\"path\":\"%s\",\"method\":\"%s\",\"statusCode\":%d,\"outcome\":\"%s\",\"requestId\":\"%s\",\"durationMs\":%d,\"requestsTotal\":%d,\"requestsOk\":%d,\"requestsKo\":%d,\"averageLatencyMs\":%d}",
                escapedEvent,
                escapedPath,
                escapedMethod,
                statusCode,
                escapedOutcome,
                escapedRequestId,
                durationMs,
                metrics.requestsTotal(),
                metrics.requestsOk(),
                metrics.requestsKo(),
                metrics.averageLatencyMs()));
    }

    record RuntimeMetricsSnapshot(long requestsTotal, long requestsOk, long requestsKo, long averageLatencyMs) {
    }

    static final class RuntimeMetrics {
        private final AtomicLong requestsTotal = new AtomicLong();
        private final AtomicLong requestsOk = new AtomicLong();
        private final AtomicLong requestsKo = new AtomicLong();
        private final AtomicLong latencyTotalMs = new AtomicLong();

        RuntimeMetricsSnapshot record(int statusCode, long latencyMs) {
            requestsTotal.incrementAndGet();
            if (statusCode >= 200 && statusCode < 400) {
                requestsOk.incrementAndGet();
            } else {
                requestsKo.incrementAndGet();
            }
            latencyTotalMs.addAndGet(Math.max(latencyMs, 0L));
            return snapshot();
        }

        RuntimeMetricsSnapshot snapshot() {
            long total = requestsTotal.get();
            long ok = requestsOk.get();
            long ko = requestsKo.get();
            long avg = total == 0L ? 0L : latencyTotalMs.get() / total;
            return new RuntimeMetricsSnapshot(total, ok, ko, avg);
        }
    }

    static void logRoomPersistenceFailure(String code, String requestId, RuntimeException failure) {
        Throwable cause = failure.getCause();
        String sqlState = cause instanceof SQLException sqlFailure ? sqlFailure.getSQLState() : "n/a";
        System.err.println(String.format(Locale.ROOT,
                "{\"event\":\"room_persistence_failure\",\"code\":\"%s\",\"requestId\":\"%s\",\"sqlState\":\"%s\"}",
                escapeJson(code), escapeJson(requestId), escapeJson(sqlState)));
    }
}
