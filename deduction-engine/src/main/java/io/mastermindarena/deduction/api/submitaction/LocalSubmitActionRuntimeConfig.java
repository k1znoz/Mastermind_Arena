package io.mastermindarena.deduction.api.submitaction;

import java.util.Objects;

public record LocalSubmitActionRuntimeConfig(
        int port,
        String path,
        String apiKeyHeaderName,
        String apiKeyValue,
    String requestIdHeaderName,
    String corsAllowedOrigins,
    String corsAllowedMethods,
    String corsAllowedHeaders,
    int corsMaxAgeSeconds,
    String dbUrl,
    String dbUser,
    String dbPassword,
    String dbSchema,
        String seedMatchId,
        String seedActorId,
        String seedOpponentId
) {
    public LocalSubmitActionRuntimeConfig {
        Objects.requireNonNull(path, "path is required");
        Objects.requireNonNull(apiKeyHeaderName, "apiKeyHeaderName is required");
        Objects.requireNonNull(apiKeyValue, "apiKeyValue is required");
        Objects.requireNonNull(requestIdHeaderName, "requestIdHeaderName is required");
        Objects.requireNonNull(corsAllowedOrigins, "corsAllowedOrigins is required");
        Objects.requireNonNull(corsAllowedMethods, "corsAllowedMethods is required");
        Objects.requireNonNull(corsAllowedHeaders, "corsAllowedHeaders is required");
        Objects.requireNonNull(dbUrl, "dbUrl is required");
        Objects.requireNonNull(dbUser, "dbUser is required");
        Objects.requireNonNull(dbPassword, "dbPassword is required");
        Objects.requireNonNull(dbSchema, "dbSchema is required");
        Objects.requireNonNull(seedMatchId, "seedMatchId is required");
        Objects.requireNonNull(seedActorId, "seedActorId is required");
        Objects.requireNonNull(seedOpponentId, "seedOpponentId is required");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        if (apiKeyHeaderName.isBlank()) {
            throw new IllegalArgumentException("apiKeyHeaderName must not be blank");
        }
        if (apiKeyValue.isBlank()) {
            throw new IllegalArgumentException("apiKeyValue must not be blank");
        }
        requireStrongApiKey(apiKeyValue);
        if (requestIdHeaderName.isBlank()) {
            throw new IllegalArgumentException("requestIdHeaderName must not be blank");
        }
        if (corsAllowedOrigins.isBlank()) {
            throw new IllegalArgumentException("corsAllowedOrigins must not be blank");
        }
        if (corsAllowedMethods.isBlank()) {
            throw new IllegalArgumentException("corsAllowedMethods must not be blank");
        }
        if (corsAllowedHeaders.isBlank()) {
            throw new IllegalArgumentException("corsAllowedHeaders must not be blank");
        }
        if (corsMaxAgeSeconds < 0) {
            throw new IllegalArgumentException("corsMaxAgeSeconds must be >= 0");
        }
        if (dbUrl.isBlank()) {
            throw new IllegalArgumentException("dbUrl must not be blank");
        }
        if (dbUser.isBlank()) {
            throw new IllegalArgumentException("dbUser must not be blank");
        }
        if (dbSchema.isBlank()) {
            throw new IllegalArgumentException("dbSchema must not be blank");
        }
        requireSupabaseSslMode(dbUrl);
    }

    public static LocalSubmitActionRuntimeConfig fromEnvironment() {
        return new LocalSubmitActionRuntimeConfig(
                intValue("submitAction.port", new String[]{"APP_HTTP_PORT", "SUBMIT_ACTION_PORT"}, 8080),
                stringValue("submitAction.path", new String[]{"SUBMIT_ACTION_PATH"}, LocalSubmitActionEndpoint.PATH),
                stringValue("submitAction.auth.apiKeyHeader", new String[]{"APP_API_KEY_HEADER", "SUBMIT_ACTION_API_KEY_HEADER"}, "X-API-Key"),
                stringValue("submitAction.auth.apiKeyValue", new String[]{"APP_API_KEY", "SUBMIT_ACTION_API_KEY_VALUE"}, "dev-submit-action-key"),
                stringValue("submitAction.observability.requestIdHeader", new String[]{"APP_REQUEST_ID_HEADER"}, "X-Request-Id"),
                stringValue("submitAction.http.cors.allowedOrigins", new String[]{"APP_CORS_ALLOWED_ORIGINS", "SUBMIT_ACTION_CORS_ALLOWED_ORIGINS"}, "http://localhost:5173"),
                stringValue("submitAction.http.cors.allowedMethods", new String[]{"APP_CORS_ALLOWED_METHODS", "SUBMIT_ACTION_CORS_ALLOWED_METHODS"}, "GET,POST,OPTIONS"),
                stringValue("submitAction.http.cors.allowedHeaders", new String[]{"APP_CORS_ALLOWED_HEADERS", "SUBMIT_ACTION_CORS_ALLOWED_HEADERS"}, "Content-Type,X-API-Key,X-Request-Id"),
                intValue("submitAction.http.cors.maxAgeSeconds", new String[]{"APP_CORS_MAX_AGE_SECONDS", "SUBMIT_ACTION_CORS_MAX_AGE_SECONDS"}, 600),
                stringValue("submitAction.db.url", new String[]{"APP_DB_URL", "SUBMIT_ACTION_DB_URL"}, "jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require"),
                stringValue("submitAction.db.user", new String[]{"APP_DB_USER", "SUBMIT_ACTION_DB_USER"}, "postgres"),
                stringValue("submitAction.db.password", new String[]{"APP_DB_PASSWORD", "SUBMIT_ACTION_DB_PASSWORD"}, "change-me-db-password"),
                stringValue("submitAction.db.schema", new String[]{"APP_DB_SCHEMA", "SUBMIT_ACTION_DB_SCHEMA"}, "public"),
                stringValue("submitAction.seed.matchId", new String[]{"SUBMIT_ACTION_SEED_MATCH_ID"}, "local-match"),
                stringValue("submitAction.seed.actorId", new String[]{"SUBMIT_ACTION_SEED_ACTOR_ID"}, "p1"),
                stringValue("submitAction.seed.opponentId", new String[]{"SUBMIT_ACTION_SEED_OPPONENT_ID"}, "p2")
        );
    }

    public static String resolutionRule() {
        return "system property first, then known environment aliases, then default value";
    }

    private static int intValue(String propertyName, String[] envNames, int defaultValue) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            raw = firstEnvironmentValue(envNames);
        }
        return raw == null || raw.isBlank() ? defaultValue : Integer.parseInt(raw);
    }

    private static String stringValue(String propertyName, String[] envNames, String defaultValue) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            raw = firstEnvironmentValue(envNames);
        }
        return raw == null || raw.isBlank() ? defaultValue : raw;
    }

    private static String firstEnvironmentValue(String[] envNames) {
        for (String envName : envNames) {
            String raw = System.getenv(envName);
            if (raw != null && !raw.isBlank()) {
                return raw;
            }
        }
        return null;
    }

    private static void requireSupabaseSslMode(String dbUrl) {
        String normalized = dbUrl.toLowerCase();
        if (normalized.contains(".supabase.co") && !normalized.matches(".*[?&]sslmode=require([&#].*)?$")) {
            throw new IllegalArgumentException("Supabase PostgreSQL URL must include sslmode=require");
        }
    }

    private static void requireStrongApiKey(String apiKeyValue) {
        String normalized = apiKeyValue.trim().toLowerCase();
        if (normalized.length() < 16) {
            throw new IllegalArgumentException("apiKeyValue must be at least 16 characters long");
        }

        if (normalized.equals("dev-submit-action-key")
                || normalized.equals("dev-local-key-123")
                || normalized.equals("change-me-dev-key")
                || normalized.equals("change-me-api-key")) {
            throw new IllegalArgumentException("apiKeyValue must not use known weak default values");
        }
    }
}
