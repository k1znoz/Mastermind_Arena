package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.contract.RuleSet;
import io.mastermindarena.deduction.engine.workflow.RuleEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LocalMastermindRuleSet implements RuleSet {
    private static final int CODE_LENGTH = 4;
    private static final Set<String> ALLOWED_SYMBOLS = Set.of("A", "B", "C", "D", "E", "F", "G", "H");

    @Override
    public ActionResolution resolve(Object actionInput) {
        if (!(actionInput instanceof RuleEvaluationContext context)) {
            return reject("INVALID_ACTION_CONTEXT");
        }

        Object payload = context.command().actionPayload();
        if (!(payload instanceof Map<?, ?> rawMap)) {
            return reject("INVALID_ACTION_PAYLOAD");
        }

        String actionType = readActionType(rawMap);
        if (!"SUBMIT_GUESS".equals(actionType)) {
            return reject("UNSUPPORTED_ACTION_TYPE");
        }

        List<String> guess = extractSymbols(rawMap.get("guess"));
        if (!isValidCode(guess)) {
            return reject("INVALID_GUESS");
        }

        return acceptAndAwaitFeedback();
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

    private static ActionResolution acceptAndAwaitFeedback() {
        return ActionResolution.of(Set.of(
                EngineDirective.ACCEPT_ACTION,
                EngineDirective.CONTINUE_TURN
        ));
    }
}
