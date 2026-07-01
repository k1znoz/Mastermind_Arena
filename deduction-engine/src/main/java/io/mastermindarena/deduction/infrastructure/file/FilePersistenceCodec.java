package io.mastermindarena.deduction.infrastructure.file;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.CancellationReason;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.MatchOutcome;
import io.mastermindarena.deduction.engine.contract.ParticipantResult;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class FilePersistenceCodec {
    private FilePersistenceCodec() {
    }

    static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value.strip()), StandardCharsets.UTF_8);
    }

    static String encodeStringList(List<String> values) {
        return values.stream().map(FilePersistenceCodec::encode).collect(Collectors.joining(","));
    }

    static List<String> decodeStringList(String encodedList) {
        if (encodedList == null || encodedList.isBlank()) {
            return List.of();
        }
        String[] chunks = encodedList.split(",", -1);
        List<String> result = new ArrayList<>(chunks.length);
        for (String chunk : chunks) {
            result.add(decode(chunk));
        }
        return List.copyOf(result);
    }

    static String serializeMatchRuntimeState(MatchRuntimeState state) {
        String cancellationCode = state.cancellationReasonOptional().map(CancellationReason::code).orElse("");
        String outcome = state.matchOutcomeOptional().map(FilePersistenceCodec::serializeMatchOutcome).orElse("");

        return String.join("\t",
                encode(state.matchId()),
                Integer.toString(state.turnNumber()),
                Integer.toString(state.currentActorIndex()),
                Boolean.toString(state.turnActive()),
                encodeStringList(state.actorOrder()),
                Long.toString(state.version()),
                encode(state.status()),
                encode(cancellationCode),
                encode(outcome)
        );
    }

    static MatchRuntimeState deserializeMatchRuntimeState(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 9) {
            throw new IllegalArgumentException("Invalid MatchRuntimeState payload");
        }

        String matchId = decode(fields[0]);
        int turnNumber = Integer.parseInt(fields[1]);
        int currentActorIndex = Integer.parseInt(fields[2]);
        boolean turnActive = Boolean.parseBoolean(fields[3]);
        List<String> actorOrder = decodeStringList(fields[4]);
        long version = Long.parseLong(fields[5]);
        String status = decode(fields[6]);

        String cancellationCode = decode(fields[7]);
        CancellationReason cancellationReason = cancellationCode.isBlank() ? null : new CancellationReason(cancellationCode);

        String outcomeRaw = decode(fields[8]);
        MatchOutcome outcome = outcomeRaw.isBlank() ? null : deserializeMatchOutcome(outcomeRaw);

        return new MatchRuntimeState(
                matchId,
                turnNumber,
                currentActorIndex,
                turnActive,
                actorOrder,
                version,
                status,
                outcome,
                cancellationReason
        );
    }

    static String serializeActionResolution(ActionResolution resolution) {
        String directives = resolution.engineDirectives().stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));

        Optional<Rejection> rejectionOptional = resolution.rejection();
        String rejectionOrigin = rejectionOptional.map(r -> r.origin().name()).orElse("");
        String rejectionCode = rejectionOptional.map(Rejection::code).orElse("");
        String rejectionMessageKey = rejectionOptional.map(r -> r.messageKey() == null ? "" : r.messageKey()).orElse("");
        String rejectionTargets = rejectionOptional
                .map(r -> r.targetLogs().stream().map(Enum::name).sorted().collect(Collectors.joining(",")))
                .orElse("");

        String cancellationCode = resolution.cancellationReason().map(CancellationReason::code).orElse("");
        String matchOutcome = resolution.matchOutcome().map(FilePersistenceCodec::serializeMatchOutcome).orElse("");

        return String.join("\t",
                encode(directives),
                encode(rejectionOrigin),
                encode(rejectionCode),
                encode(rejectionMessageKey),
                encode(rejectionTargets),
                encode(cancellationCode),
                encode(matchOutcome)
        );
    }

    static ActionResolution deserializeActionResolution(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 7) {
            throw new IllegalArgumentException("Invalid ActionResolution payload");
        }

        String directivesRaw = decode(fields[0]);
        Set<EngineDirective> directives = directivesRaw.isBlank()
                ? Set.of()
                : ArraysUtil.toEnumSet(directivesRaw.split(",", -1), EngineDirective::valueOf);

        String rejectionOriginRaw = decode(fields[1]);
        String rejectionCode = decode(fields[2]);
        String rejectionMessageKey = decode(fields[3]);
        String rejectionTargetsRaw = decode(fields[4]);

        Rejection rejection = null;
        if (!rejectionOriginRaw.isBlank()) {
            Set<LogTarget> targetLogs = rejectionTargetsRaw.isBlank()
                    ? Set.of(LogTarget.TECHNICAL_LOG)
                    : ArraysUtil.toEnumSet(rejectionTargetsRaw.split(",", -1), LogTarget::valueOf);
            rejection = new Rejection(
                    RejectionOrigin.valueOf(rejectionOriginRaw),
                    rejectionCode,
                    rejectionMessageKey.isBlank() ? null : rejectionMessageKey,
                    null,
                    targetLogs
            );
        }

        String cancellationCode = decode(fields[5]);
        CancellationReason cancellationReason = cancellationCode.isBlank() ? null : new CancellationReason(cancellationCode);

        String matchOutcomeRaw = decode(fields[6]);
        MatchOutcome matchOutcome = matchOutcomeRaw.isBlank() ? null : deserializeMatchOutcome(matchOutcomeRaw);

        return new ActionResolution(
                directives,
                null,
                matchOutcome,
                cancellationReason,
                rejection,
                List.of()
        );
    }

    static List<String> serializeSubmitActionResult(SubmitActionResult result) {
        return List.of(
                encode(result.state() == null ? "" : serializeMatchRuntimeState(result.state())),
                encode(result.resolution() == null ? "" : serializeActionResolution(result.resolution())),
                encode(encodeStringList(result.emittedEvents()))
        );
    }

    static SubmitActionResult deserializeSubmitActionResult(List<String> lines) {
        if (lines.size() != 3) {
            throw new IllegalArgumentException("Invalid SubmitActionResult payload");
        }

        String stateRaw = decode(lines.get(0));
        MatchRuntimeState state = stateRaw.isBlank() ? null : deserializeMatchRuntimeState(stateRaw);

        String resolutionRaw = decode(lines.get(1));
        ActionResolution resolution = resolutionRaw.isBlank() ? null : deserializeActionResolution(resolutionRaw);

        List<String> emittedEvents = decodeStringList(decode(lines.get(2)));

        return new SubmitActionResult(state, resolution, emittedEvents);
    }

    private static String serializeMatchOutcome(MatchOutcome outcome) {
        String participants = outcome.participantResults().stream()
                .map(p -> encode(p.participantId()) + "," + encode(p.result()) + "," + (p.rank() == null ? "" : p.rank()))
                .collect(Collectors.joining(";"));

        return String.join("|",
                encode(outcome.status()),
                encode(outcome.completionReason()),
                Long.toString(outcome.completedAt().toEpochMilli()),
                participants
        );
    }

    private static MatchOutcome deserializeMatchOutcome(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid MatchOutcome payload");
        }

        String status = decode(parts[0]);
        String completionReason = decode(parts[1]);
        Instant completedAt = Instant.ofEpochMilli(Long.parseLong(parts[2]));

        List<ParticipantResult> participants;
        if (parts[3].isBlank()) {
            participants = List.of();
        } else {
            String[] participantParts = parts[3].split(";", -1);
            List<ParticipantResult> parsed = new ArrayList<>(participantParts.length);
            for (String participantPart : participantParts) {
                String[] participantFields = participantPart.split(",", -1);
                if (participantFields.length != 3) {
                    throw new IllegalArgumentException("Invalid participant payload");
                }
                String participantId = decode(participantFields[0]);
                String result = decode(participantFields[1]);
                Integer rank = participantFields[2].isBlank() ? null : Integer.valueOf(participantFields[2]);
                parsed.add(new ParticipantResult(participantId, result, rank));
            }
            participants = List.copyOf(parsed);
        }

        return new MatchOutcome(status, completionReason, participants, completedAt);
    }

    private static final class ArraysUtil {
        private ArraysUtil() {
        }

        static <T extends Enum<T>> Set<T> toEnumSet(String[] names, java.util.function.Function<String, T> parser) {
            return java.util.Arrays.stream(names)
                    .filter(s -> !s.isBlank())
                    .map(parser)
                    .collect(Collectors.toSet());
        }
    }
}
