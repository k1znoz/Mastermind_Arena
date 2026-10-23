package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Shared HTTP encoding, bounded body reads, CORS and transport credentials. */
final class HttpExchangeSupport {
    private HttpExchangeSupport() {}

    record CorsPolicy(
            Set<String> allowedOrigins,
            boolean wildcardOrigin,
            String allowMethods,
            String allowHeaders,
            int maxAgeSeconds
    ) {
    }

    record ErrorBody(String code) {
    }

    static <T> T readBody(HttpExchange exchange, ObjectMapper json, Class<T> type) {
        try {
            byte[] bytes = exchange.getRequestBody().readNBytes(65_537);
            if (bytes.length > 65_536) throw new IllegalArgumentException("Request body too large");
            var tree = json.reader().with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(bytes);
            if (tree == null || !tree.isObject()) throw new IllegalArgumentException("JSON object required");
            RequestBodyContract.validate(tree, type);
            return json.treeToValue(tree, type);
        } catch (IOException failure) {
            throw new IllegalArgumentException("Invalid JSON request", failure);
        }
    }

    static void writeJson(HttpExchange exchange, ObjectMapper objectMapper, int statusCode, Object body) throws IOException {
        byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, json.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(json);
        }
    }

    static String resolveRequestId(HttpExchange exchange, String requestIdHeaderName) {
        String provided = exchange.getRequestHeaders().getFirst(requestIdHeaderName);
        if (provided == null || provided.isBlank()) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        }
        return provided;
    }

    static void setResponseRequestIdHeader(HttpExchange exchange, String requestIdHeaderName, String requestId) {
        exchange.getResponseHeaders().set(requestIdHeaderName, requestId);
    }

    static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static boolean secureEquals(String expected, String provided) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    static CorsPolicy createCorsPolicy(LocalSubmitActionRuntimeConfig config) {
        String rawOrigins = config.corsAllowedOrigins();
        boolean wildcard = rawOrigins.trim().equals("*");
        Set<String> origins = wildcard
                ? Set.of("*")
                : Arrays.stream(rawOrigins.split(","))
                .map(HttpExchangeSupport::normalizeOrigin)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        return new CorsPolicy(
                origins,
                wildcard,
                config.corsAllowedMethods(),
                config.corsAllowedHeaders(),
                config.corsMaxAgeSeconds()
        );
    }

    static boolean applyCors(HttpExchange exchange, CorsPolicy policy) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return true;
        }

        boolean originAllowed = policy.wildcardOrigin() || policy.allowedOrigins().contains(normalizeOrigin(requestOrigin));
        if (!originAllowed) {
            return false;
        }

        String responseOrigin = policy.wildcardOrigin() ? "*" : requestOrigin;
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", responseOrigin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", policy.allowMethods());
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", policy.allowHeaders());
        exchange.getResponseHeaders().set("Access-Control-Max-Age", Integer.toString(policy.maxAgeSeconds()));
        exchange.getResponseHeaders().add("Vary", "Origin");
        return true;
    }

    static String normalizeOrigin(String origin) {
        String normalized = origin.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static Map<String, String> queryParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        Map<String, String> params = new LinkedHashMap<>();
        Arrays.stream(rawQuery.split("&"))
                .filter(token -> !token.isBlank())
                .forEach(token -> {
                    String[] keyValue = token.split("=", 2);
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = keyValue.length > 1
                            ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                            : "";
                    if (params.putIfAbsent(key, value) != null) {
                        throw new IllegalArgumentException("Duplicate query parameter");
                    }
                });

        return Map.copyOf(params);
    }
}
