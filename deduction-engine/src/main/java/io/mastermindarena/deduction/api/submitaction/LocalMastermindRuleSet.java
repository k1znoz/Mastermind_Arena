package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.MatchOutcome;
import io.mastermindarena.deduction.engine.contract.ParticipantResult;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.RuleEvaluationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class LocalMastermindRuleSet implements RuleSet {
    private static final int CODE_LENGTH = 4;
    private static final Set<String> ALLOWED_SYMBOLS = Set.of("A", "B", "C", "D", "E", "F", "G", "H");

    @Override
    public ActionResolution resolve(Object actionInput) {
        if (!(actionInput instanceof RuleEvaluationContext context)) {
            return acceptAndPassTurn();
        }

        MatchRuntimeState state = context.state();
        String actorId = context.command().actorId();
        Object payload = context.command().actionPayload();

        if (!(payload instanceof Map<?, ?> rawMap)) {
            return acceptAndPassTurn();
        }

        String actionType = readActionType(rawMap);
        if (actionType == null) {
            return reject("INVALID_ACTION_TYPE");
        }

        List<String> actorOrder = state.actorOrder();
        if (actorOrder.size() < 2) {
            return reject("UNSUPPORTED_ACTOR_COUNT");
        }

        String opponentId = findOpponent(actorOrder, actorId);
        if (opponentId == null) {
            return reject("ACTOR_NOT_IN_MATCH");
        }

        Set<String> readyActors = collectReadyActors(state.actionLog());
        Map<String, List<String>> secretsByActor = collectSecretsByActor(state.actionLog());

        return switch (actionType) {
            case "PLAYER_READY" -> resolvePlayerReady(actorId, readyActors);
            case "SECRET_CODE_SET" -> resolveSecretCodeSet(rawMap, actorId, readyActors, secretsByActor);
            case "SUBMIT_GUESS" -> resolveSubmitGuess(rawMap, state, actorId, opponentId, readyActors, secretsByActor);
            default -> reject("UNSUPPORTED_ACTION_TYPE");
        };
    }

    private ActionResolution resolvePlayerReady(String actorId, Set<String> readyActors) {
        if (readyActors.contains(actorId)) {
            return reject("PLAYER_ALREADY_READY");
        }
        return acceptAndPassTurn();
    }

    private ActionResolution resolveSecretCodeSet(
            Map<?, ?> payload,
            String actorId,
            Set<String> readyActors,
            Map<String, List<String>> secretsByActor
    ) {
        if (!readyActors.contains(actorId)) {
            return reject("PLAYER_NOT_READY");
        }
        if (secretsByActor.containsKey(actorId)) {
            return reject("SECRET_ALREADY_SET");
        }

        List<String> secret = extractSymbols(payload.get("secretCode"));
        if (!isValidCode(secret)) {
            return reject("INVALID_SECRET_CODE");
        }

        return acceptAndPassTurn();
    }

    private ActionResolution resolveSubmitGuess(
            Map<?, ?> payload,
            MatchRuntimeState state,
            String actorId,
            String opponentId,
            Set<String> readyActors,
            Map<String, List<String>> secretsByActor
    ) {
        if (!readyActors.contains(actorId) || !readyActors.contains(opponentId)) {
            return reject("PLAYERS_NOT_READY");
        }
        if (!secretsByActor.containsKey(actorId)) {
            return reject("ACTOR_SECRET_NOT_SET");
        }
        if (!secretsByActor.containsKey(opponentId)) {
            return reject("OPPONENT_SECRET_NOT_SET");
        }

        List<String> guess = extractSymbols(payload.get("guess"));
        if (!isValidCode(guess)) {
            return reject("INVALID_GUESS");
        }

        Map<String, Boolean> solvedByActor = computeSolvedByActor(state.actionLog(), secretsByActor, state.actorOrder());
        boolean actorJustSolved = Objects.equals(guess, secretsByActor.get(opponentId));
        if (actorJustSolved) {
            solvedByActor.put(actorId, true);
        }

        boolean everyoneSolved = state.actorOrder().stream().allMatch(a -> solvedByActor.getOrDefault(a, false));
        if (!everyoneSolved) {
            return acceptAndPassTurn();
        }

        String winnerId = findWinner(state.actionLog(), state.actorOrder(), secretsByActor, actorId, actorJustSolved, guess);
        String loserId = state.actorOrder().stream().filter(a -> !a.equals(winnerId)).findFirst().orElse(actorId);

        MatchOutcome outcome = new MatchOutcome(
                "FINISHED",
                "BOTH_CODES_CRACKED",
                List.of(
                        new ParticipantResult(winnerId, "WIN", 1),
                        new ParticipantResult(loserId, "LOSE", 2)
                ),
                Instant.now()
        );

        return new ActionResolution(
                Set.of(EngineDirective.ACCEPT_ACTION, EngineDirective.FINISH_MATCH),
                null,
                outcome,
                null,
                null,
                List.of()
        );
    }

    private static String findWinner(
            List<MatchActionRecord> actionLog,
            List<String> actorOrder,
            Map<String, List<String>> secretsByActor,
            String currentActor,
            boolean currentActorSolved,
            List<String> currentGuess
    ) {
        List<GuessAttempt> attempts = new ArrayList<>();
        for (MatchActionRecord entry : actionLog) {
            if (!"SUBMIT_GUESS".equals(entry.actionType())) {
                continue;
            }
            attempts.add(new GuessAttempt(entry.actorId(), entry.symbols()));
        }
        if (currentActorSolved) {
            attempts.add(new GuessAttempt(currentActor, currentGuess));
        }

        for (GuessAttempt attempt : attempts) {
            String opponent = findOpponent(actorOrder, attempt.actorId());
            if (opponent == null) {
                continue;
            }
            List<String> opponentSecret = secretsByActor.get(opponent);
            if (opponentSecret != null && opponentSecret.equals(attempt.guess())) {
                return attempt.actorId();
            }
        }

        return currentActor;
    }

    private static Map<String, Boolean> computeSolvedByActor(
            List<MatchActionRecord> actionLog,
            Map<String, List<String>> secretsByActor,
            List<String> actorOrder
    ) {
        Map<String, Boolean> solved = new HashMap<>();
        for (String actor : actorOrder) {
            solved.put(actor, false);
        }

        for (MatchActionRecord entry : actionLog) {
            if (!"SUBMIT_GUESS".equals(entry.actionType())) {
                continue;
            }
            String actorId = entry.actorId();
            String opponentId = findOpponent(actorOrder, actorId);
            if (opponentId == null) {
                continue;
            }
            List<String> opponentSecret = secretsByActor.get(opponentId);
            if (opponentSecret != null && opponentSecret.equals(entry.symbols())) {
                solved.put(actorId, true);
            }
        }

        return solved;
    }

    private static Set<String> collectReadyActors(List<MatchActionRecord> actionLog) {
        Set<String> readyActors = new HashSet<>();
        for (MatchActionRecord entry : actionLog) {
            if ("PLAYER_READY".equals(entry.actionType())) {
                readyActors.add(entry.actorId());
            }
        }
        return readyActors;
    }

    private static Map<String, List<String>> collectSecretsByActor(List<MatchActionRecord> actionLog) {
        Map<String, List<String>> secrets = new HashMap<>();
        for (MatchActionRecord entry : actionLog) {
            if ("SECRET_CODE_SET".equals(entry.actionType()) && !entry.symbols().isEmpty()) {
                secrets.putIfAbsent(entry.actorId(), List.copyOf(entry.symbols()));
            }
        }
        return secrets;
    }

    private static List<String> extractSymbols(Object rawSymbols) {
        if (!(rawSymbols instanceof List<?> list)) {
            return List.of();
        }

        List<String> symbols = new ArrayList<>(list.size());
        for (Object item : list) {
            String symbol = String.valueOf(item).trim().toUpperCase();
            symbols.add(symbol);
        }
        return List.copyOf(symbols);
    }

    private static boolean isValidCode(List<String> symbols) {
        if (symbols.size() != CODE_LENGTH) {
            return false;
        }
        return symbols.stream().allMatch(ALLOWED_SYMBOLS::contains);
    }

    private static String readActionType(Map<?, ?> payload) {
        Object type = payload.get("type");
        if (type == null) {
            return null;
        }
        String normalized = String.valueOf(type).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String findOpponent(List<String> actorOrder, String actorId) {
        for (String candidate : actorOrder) {
            if (!candidate.equals(actorId)) {
                return candidate;
            }
        }
        return null;
    }

    private static ActionResolution reject(String code) {
        Rejection rejection = new Rejection(
                RejectionOrigin.RULESET,
                code,
                null,
                null,
                Set.of(LogTarget.MATCH_HISTORY)
        );
        return new ActionResolution(
                Set.of(EngineDirective.REJECT_ACTION),
                null,
                null,
                null,
                rejection,
                List.of()
        );
    }

    private static ActionResolution acceptAndPassTurn() {
        return ActionResolution.of(Set.of(
                EngineDirective.ACCEPT_ACTION,
                EngineDirective.END_TURN,
                EngineDirective.START_NEXT_TURN
        ));
    }

    private record GuessAttempt(String actorId, List<String> guess) {
    }
}
