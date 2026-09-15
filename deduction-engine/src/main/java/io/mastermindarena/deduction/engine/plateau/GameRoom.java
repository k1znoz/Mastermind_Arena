package io.mastermindarena.deduction.engine.plateau;

import io.mastermindarena.deduction.engine.workflow.MatchRuntimeState;

import java.util.List;
import java.util.Objects;

public record GameRoom(
        MatchRuntimeState runtimeState,
        List<Player> players,
        List<Turn> turns
) {
    public GameRoom {
        Objects.requireNonNull(runtimeState, "runtimeState is required");
        Objects.requireNonNull(players, "players is required");
        Objects.requireNonNull(turns, "turns is required");
        players = List.copyOf(players);
        turns = List.copyOf(turns);

        if (players.size() != 2) {
            throw new IllegalArgumentException("a game room must contain exactly two players");
        }
        if (players.get(0).equals(players.get(1))) {
            throw new IllegalArgumentException("players must be distinct");
        }
        if (!runtimeState.actorOrder().equals(players.stream().map(Player::id).toList())) {
            throw new IllegalArgumentException("players must match runtime actor order");
        }
    }
}