package io.mastermindarena.deduction.infrastructure.file;

import io.mastermindarena.deduction.engine.contract.ActionResolution;
import io.mastermindarena.deduction.engine.contract.CancellationReason;
import io.mastermindarena.deduction.engine.contract.EngineDirective;
import io.mastermindarena.deduction.engine.contract.LogTarget;
import io.mastermindarena.deduction.engine.contract.MatchOutcome;
import io.mastermindarena.deduction.engine.contract.ParticipantResult;
import io.mastermindarena.deduction.engine.contract.Rejection;
import io.mastermindarena.deduction.engine.contract.RejectionOrigin;
import io.mastermindarena.deduction.engine.workflow.MatchActionRecord;
import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;
import io.mastermindarena.deduction.engine.workflow.SubmitActionCommand;
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

public final class FilePersistenceCodec {
    // separateur interne (non present en base64) pour combiner deux blobs deja encodes dans une seule ligne
    private static final String FIELD_GROUP_SEPARATOR = "\u001F";

    private FilePersistenceCodec() {
    }

    public static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value.strip()), StandardCharsets.UTF_8);
    }

    public static String encodeStringList(List<String> values) {
        return values.stream().map(FilePersistenceCodec::encode).collect(Collectors.joining(","));
    }

    public static List<String> decodeStringList(String encodedList) {
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

    public static String serializeMatchRuntimeState(MatchRuntimeState state) {
        String legacyOutcome = state.legacyMatchOutcomeOptional()
                .map(FilePersistenceCodec::serializeLegacyMatchOutcome)
                .orElse("");

        return String.join("\t",
                encode(state.matchId()),
                encode(state.status()),
                Integer.toString(state.turnNumber()),
                Integer.toString(state.currentActorIndex()),
                Integer.toString(state.currentFeedbackActorIndex()),
                Boolean.toString(state.turnActive()),
                encodeStringList(state.actorOrder()),
                encode(state.activePlayerId()),
                encode(state.feedbackPlayerId()),
                Long.toString(state.version()),
                encodeStringList(state.actionLog().stream().map(FilePersistenceCodec::serializeMatchActionRecord).toList()),
                encode(legacyOutcome)
        );
    }

    public static MatchRuntimeState deserializeMatchRuntimeState(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 12) {
            throw new IllegalArgumentException("Invalid MatchRuntimeState payload");
        }

        String matchId = decode(fields[0]);
        String status = decode(fields[1]);
        int turnNumber = Integer.parseInt(fields[2]);
        int currentActorIndex = Integer.parseInt(fields[3]);
        int currentFeedbackActorIndex = Integer.parseInt(fields[4]);
        boolean turnActive = Boolean.parseBoolean(fields[5]);
        List<String> actorOrder = decodeStringList(fields[6]);
        String activePlayerId = decode(fields[7]);
        String feedbackPlayerId = decode(fields[8]);
        long version = Long.parseLong(fields[9]);
        List<MatchActionRecord> actionLog = decodeStringList(fields[10]).stream()
                .map(FilePersistenceCodec::deserializeMatchActionRecord)
                .toList();

        String legacyOutcomeRaw = decode(fields[11]);
        MatchRuntimeState.MatchOutcome legacyOutcome = legacyOutcomeRaw.isBlank() ? null : deserializeLegacyMatchOutcome(legacyOutcomeRaw);

        return new MatchRuntimeState(
                matchId,
                status,
                turnNumber,
                currentActorIndex,
                currentFeedbackActorIndex,
                turnActive,
                actorOrder,
                activePlayerId,
                feedbackPlayerId,
                version,
                actionLog,
                legacyOutcome
        );
    }

    private static String serializeLegacyMatchOutcome(MatchRuntimeState.MatchOutcome outcome) {
        return String.join("|",
                encode(outcome.status()),
                encode(outcome.reason() == null ? "" : outcome.reason())
        );
    }

    private static MatchRuntimeState.MatchOutcome deserializeLegacyMatchOutcome(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid MatchOutcome payload");
        }
        String status = decode(parts[0]);
        String reasonRaw = decode(parts[1]);
        return new MatchRuntimeState.MatchOutcome(status, reasonRaw.isBlank() ? null : reasonRaw);
    }

    /** Conserve pour compatibilite locale : ActionResolution n'est plus produit par le workflow principal. */
    public static String serializeActionResolution(ActionResolution resolution) {
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

    public static ActionResolution deserializeActionResolution(String line) {
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

    public static List<String> serializeSubmitActionResult(SubmitActionResult result) {
        return List.of(
                encode(result.state() == null ? "" : serializeMatchRuntimeState(result.state())),
                encode(serializeActionAndRejection(result.action(), result.rejection())),
                encode(encodeStringList(result.emittedEvents()))
        );
    }

    public static SubmitActionResult deserializeSubmitActionResult(List<String> lines) {
        if (lines.size() != 3) {
            throw new IllegalArgumentException("Invalid SubmitActionResult payload");
        }

        String stateRaw = decode(lines.get(0));
        MatchRuntimeState state = stateRaw.isBlank() ? null : deserializeMatchRuntimeState(stateRaw);

        String[] actionAndRejection = decode(lines.get(1)).split(FIELD_GROUP_SEPARATOR, -1);
        if (actionAndRejection.length != 2) {
            throw new IllegalArgumentException("Invalid SubmitActionResult action/rejection payload");
        }
        SubmitActionCommand action = deserializeSubmitActionCommand(actionAndRejection[0]);
        Rejection rejection = actionAndRejection[1].isBlank() ? null : deserializeRejection(actionAndRejection[1]);

        List<String> emittedEvents = decodeStringList(decode(lines.get(2)));

        return new SubmitActionResult(state, action, emittedEvents, rejection);
    }

    private static String serializeActionAndRejection(SubmitActionCommand action, Rejection rejection) {
        return String.join(FIELD_GROUP_SEPARATOR,
                serializeSubmitActionCommand(action),
                rejection == null ? "" : serializeRejection(rejection)
        );
    }

    private static String serializeSubmitActionCommand(SubmitActionCommand action) {
        return String.join("\t",
                encode(action.matchId()),
                encode(action.actorId()),
                Long.toString(action.expectedVersion()),
                encode(action.idempotencyKey()),
                encode(action.actionType()),
                encode(action.payload() == null ? "" : action.payload()),
                encode(action.feedback() == null ? "" : action.feedback())
        );
    }

    private static SubmitActionCommand deserializeSubmitActionCommand(String value) {
        String[] fields = value.split("\t", -1);
        if (fields.length != 7) {
            throw new IllegalArgumentException("Invalid SubmitActionCommand payload");
        }

        String payload = decode(fields[5]);
        String feedback = decode(fields[6]);
        return new SubmitActionCommand(
                decode(fields[0]),
                decode(fields[1]),
                Long.parseLong(fields[2]),
                decode(fields[3]),
                decode(fields[4]),
                payload.isBlank() ? null : payload,
                feedback.isBlank() ? null : feedback
        );
    }

    private static String serializeRejection(Rejection rejection) {
        String targets = rejection.targetLogs().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        return String.join("\t",
                rejection.origin().name(),
                encode(rejection.code()),
                encode(rejection.messageKey() == null ? "" : rejection.messageKey()),
                encode(targets)
        );
    }

    private static Rejection deserializeRejection(String value) {
        String[] fields = value.split("\t", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Invalid Rejection payload");
        }

        RejectionOrigin origin = RejectionOrigin.valueOf(fields[0]);
        String code = decode(fields[1]);
        String messageKey = decode(fields[2]);
        String targetsRaw = decode(fields[3]);
        Set<LogTarget> targetLogs = targetsRaw.isBlank()
                ? Set.of(LogTarget.TECHNICAL_LOG)
                : ArraysUtil.toEnumSet(targetsRaw.split(",", -1), LogTarget::valueOf);

        return new Rejection(origin, code, messageKey.isBlank() ? null : messageKey, null, targetLogs);
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

    private static String serializeMatchActionRecord(MatchActionRecord actionRecord) {
        return String.join("|",
                encode(actionRecord.actorId()),
                encode(actionRecord.actionType()),
                encode(actionRecord.guess() == null ? "" : actionRecord.guess()),
                encode(actionRecord.feedback() == null ? "" : actionRecord.feedback()),
                Long.toString(actionRecord.timestamp()),
                Long.toString(actionRecord.version())
        );
    }

    private static MatchActionRecord deserializeMatchActionRecord(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid MatchActionRecord payload");
        }

        String guess = decode(parts[2]);
        String feedback = decode(parts[3]);
        return new MatchActionRecord(
                decode(parts[0]),
                decode(parts[1]),
                guess.isBlank() ? null : guess,
                feedback.isBlank() ? null : feedback,
                Long.parseLong(parts[4]),
                Long.parseLong(parts[5])
        );
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
