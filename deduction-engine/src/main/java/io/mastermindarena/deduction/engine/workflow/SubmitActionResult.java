package io.mastermindarena.deduction.engine.workflow;

import io.mastermindarena.deduction.engine.contract.ActionResolution;

import java.util.List;

public record SubmitActionResult(
        MatchRuntimeState state,
        ActionResolution resolution,
        List<String> emittedEvents
) {
}
