package io.mastermindarena.deduction.engine.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rules for this game's simultaneous boards. Indices are declared, never calculated from secrets. */
public final class MastermindBoardRules {
    private static final ObjectMapper JSON = new ObjectMapper();
    public static final List<String> COLORS = List.of("A", "B", "C", "D", "E", "F");

    private MastermindBoardRules() {}

    public static void validateCode(String raw) {
        JsonNode code = read(raw, "INVALID_CODE");
        if (!code.isArray() || code.size() != 4) throw new IllegalStateException("INVALID_CODE");
        for (JsonNode color : code) {
            if (!color.isTextual() || !COLORS.contains(color.textValue())) throw new IllegalStateException("INVALID_CODE");
        }
    }

    public static int blackPegs(String raw) {
        JsonNode node = read(raw, "INVALID_FEEDBACK");
        if (!node.isObject() || !node.path("bienPlaces").isIntegralNumber()
                || !node.path("bienPlaces").canConvertToInt() || !node.path("malPlaces").isIntegralNumber()
                || !node.path("malPlaces").canConvertToInt()) throw new IllegalStateException("INVALID_FEEDBACK");
        int black = node.path("bienPlaces").intValue();
        int white = node.path("malPlaces").intValue();
        if (black < 0 || black > 4 || white < 0 || white > 4 || black + white > 4) {
            throw new IllegalStateException("INVALID_FEEDBACK");
        }
        return black;
    }

    public static long feedbackTarget(String raw) {
        JsonNode target = read(raw, "INVALID_FEEDBACK_TARGET").path("feedbackVersion");
        if (!target.isIntegralNumber() || !target.canConvertToLong() || target.longValue() <= 0) {
            throw new IllegalStateException("INVALID_FEEDBACK_TARGET");
        }
        return target.longValue();
    }

    private static JsonNode read(String raw, String error) {
        try {
            JsonNode node = raw == null ? null : JSON.reader().with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(raw);
            if (node == null) throw new IllegalStateException(error);
            return node;
        } catch (IOException failure) {
            throw new IllegalStateException(error, failure);
        }
    }

    /** Older persisted actions did not carry a round; infer it from their original sequence. */
    public static List<MatchActionRecord> effectiveActions(MatchRuntimeState state) {
        Map<String, Integer> rounds = new HashMap<>();
        Map<Long, MatchActionRecord> originals = new LinkedHashMap<>();
        List<MatchActionRecord> notifications = new ArrayList<>();
        for (MatchActionRecord action : state.actionLog()) {
            if (SubmitActionCommand.CORRECT_FEEDBACK.equals(action.actionType())) {
                long target = feedbackTarget(action.guess());
                MatchActionRecord original = originals.get(target);
                if (original != null) originals.put(target, new MatchActionRecord(original.actorId(), original.actionType(),
                        original.guess(), action.feedback(), original.timestamp(), original.version(), original.roundNumber()));
                notifications.add(action);
            } else if (SubmitActionCommand.CONTEST_FEEDBACK.equals(action.actionType())) {
                notifications.add(action);
            } else {
                String key = action.actorId() + ":" + action.actionType();
                int inferred = rounds.getOrDefault(key, 0);
                rounds.put(key, inferred + 1);
                int round = action.roundNumber() >= 0 ? action.roundNumber() : inferred;
                originals.put(action.version(), new MatchActionRecord(action.actorId(), action.actionType(), action.guess(),
                        action.feedback(), action.timestamp(), action.version(), round));
            }
        }
        List<MatchActionRecord> result = new ArrayList<>(originals.values());
        result.addAll(notifications);
        result.sort(java.util.Comparator.comparingLong(MatchActionRecord::version));
        return List.copyOf(result);
    }

    public static List<String> solvedPlayers(MatchRuntimeState state) {
        return effectiveActions(state).stream().filter(action -> SubmitActionCommand.SEND_FEEDBACK.equals(action.actionType())
                        && action.roundNumber() < state.turnNumber() && blackPegs(action.feedback()) == 4)
                .map(action -> opponent(state, action.actorId())).distinct().toList();
    }

    public static List<String> guessers(MatchRuntimeState state) {
        List<String> solved = solvedPlayers(state);
        return state.actorOrder().stream().filter(id -> !solved.contains(id)).toList();
    }

    public static List<String> feedbackGivers(MatchRuntimeState state) {
        return guessers(state).stream().map(id -> opponent(state, id)).toList();
    }

    public static String opponent(MatchRuntimeState state, String actorId) {
        return state.actorOrder().stream().filter(id -> !id.equals(actorId)).findFirst().orElseThrow();
    }

    public static boolean hasActed(MatchRuntimeState state, String actorId, String type) {
        return effectiveActions(state).stream().anyMatch(action -> type.equals(action.actionType())
                && actorId.equals(action.actorId()) && action.roundNumber() == state.turnNumber());
    }

    public static String result(MatchRuntimeState state) {
        Map<String, Integer> solvedAt = new HashMap<>();
        for (MatchActionRecord action : effectiveActions(state)) {
            if (SubmitActionCommand.SEND_FEEDBACK.equals(action.actionType())
                    && action.roundNumber() < state.turnNumber() && blackPegs(action.feedback()) == 4) {
                solvedAt.merge(opponent(state, action.actorId()), action.roundNumber(), Math::min);
            }
        }
        if (!solvedAt.isEmpty()) {
            int first = solvedAt.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
            List<String> winners = state.actorOrder().stream().filter(id -> solvedAt.getOrDefault(id, -1) == first).toList();
            return winners.size() == 2 ? "DRAW" : winners.get(0);
        }
        return state.actionLog().stream().filter(action -> SubmitActionCommand.ABANDON.equals(action.actionType()))
                .findFirst().map(action -> opponent(state, action.actorId())).orElse(null);
    }

    public static MatchActionRecord editableFeedback(MatchRuntimeState state, long version) {
        return effectiveActions(state).stream().filter(action -> action.version() == version
                        && SubmitActionCommand.SEND_FEEDBACK.equals(action.actionType())
                        && action.roundNumber() == state.turnNumber() - 1)
                .findFirst().orElseThrow(() -> new IllegalStateException("FEEDBACK_NOT_EDITABLE"));
    }
}
