package io.mastermindarena.deduction.api.submitaction;

import java.util.Objects;

public record LocalSubmitActionRuntimeConfig(
        int port,
        String path,
    String authBearerToken,
    String persistenceNode,
        String seedMatchId,
        String seedActorId,
        String seedOpponentId
) {
    public LocalSubmitActionRuntimeConfig {
        Objects.requireNonNull(path, "path is required");
        Objects.requireNonNull(authBearerToken, "authBearerToken is required");
        Objects.requireNonNull(persistenceNode, "persistenceNode is required");
        Objects.requireNonNull(seedMatchId, "seedMatchId is required");
        Objects.requireNonNull(seedActorId, "seedActorId is required");
        Objects.requireNonNull(seedOpponentId, "seedOpponentId is required");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
    }

    public static LocalSubmitActionRuntimeConfig fromEnvironment() {
        return new LocalSubmitActionRuntimeConfig(
                intValue("submitAction.port", "SUBMIT_ACTION_PORT", 8080),
                stringValue("submitAction.path", "SUBMIT_ACTION_PATH", LocalSubmitActionEndpoint.PATH),
            stringValue("submitAction.auth.bearer", "SUBMIT_ACTION_AUTH_BEARER", "dev-submit-action-token"),
            stringValue("submitAction.persistence.node", "SUBMIT_ACTION_PERSISTENCE_NODE", "/io/mastermindarena/local-submit-action"),
                stringValue("submitAction.seed.matchId", "SUBMIT_ACTION_SEED_MATCH_ID", "local-match"),
                stringValue("submitAction.seed.actorId", "SUBMIT_ACTION_SEED_ACTOR_ID", "p1"),
                stringValue("submitAction.seed.opponentId", "SUBMIT_ACTION_SEED_OPPONENT_ID", "p2")
        );
    }

    private static int intValue(String propertyName, String envName, int defaultValue) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(envName);
        }
        return raw == null || raw.isBlank() ? defaultValue : Integer.parseInt(raw);
    }

    private static String stringValue(String propertyName, String envName, String defaultValue) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(envName);
        }
        return raw == null || raw.isBlank() ? defaultValue : raw;
    }
}
