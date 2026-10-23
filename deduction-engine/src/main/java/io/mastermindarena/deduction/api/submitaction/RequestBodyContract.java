package io.mastermindarena.deduction.api.submitaction;

import com.fasterxml.jackson.databind.JsonNode;

/** Validates wire types before Jackson or application adapters can coerce values. */
final class RequestBodyContract {
    private RequestBodyContract() {}

    static void validate(JsonNode body, Class<?> type) {
        if (type == SubmitActionHttpRequest.class) {
            requiredText(body, "matchId");
            requiredText(body, "actorId");
            requiredText(body, "idempotencyKey");
            JsonNode version = body.path("expectedVersion");
            if (!version.isIntegralNumber() || !version.canConvertToLong() || version.longValue() < 0) {
                throw new IllegalArgumentException("Nonnegative integer version required");
            }
            JsonNode action = body.path("actionPayload");
            if (!action.isObject()) throw new IllegalArgumentException("Action object required");
            requiredText(action, "actionType");
            optionalText(action, "payload");
            optionalText(action, "feedback");
        } else if (type == RoomHttpHandlers.CreateRoomBody.class) {
            optionalText(body, "name");
            optionalText(body, "pseudo");
            optionalText(body, "accessCode");
        } else if (type == RoomHttpHandlers.JoinRoomBody.class) {
            optionalText(body, "roomId");
            optionalText(body, "pseudo");
            optionalText(body, "accessCode");
        } else if (type == RoomHttpHandlers.ResumeRoomBody.class) {
            optionalText(body, "recoveryCode");
        } else {
            throw new IllegalArgumentException("Unsupported request contract");
        }
    }

    private static void requiredText(JsonNode body, String field) {
        JsonNode value = body.path(field);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Nonblank text required");
        }
    }

    private static void optionalText(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value != null && !value.isNull() && !value.isTextual()) {
            throw new IllegalArgumentException("Text or null required");
        }
    }
}
