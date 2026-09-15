package io.mastermindarena.deduction.infrastructure.jdbc;

import io.mastermindarena.deduction.engine.workflow.EventSink;
import io.mastermindarena.deduction.engine.workflow.GameEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcWorkflowEventSink implements EventSink {
    private final JdbcPersistenceContext context;

    public JdbcWorkflowEventSink(JdbcPersistenceContext context) {
        this.context = context;
    }

    @Override
    public void publish(GameEvent event) {
        String sql = "INSERT INTO " + context.table("workflow_event") + " (event_payload) VALUES (?)";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist workflow event", e);
        }
    }

    @Override
    public List<GameEvent> allEvents() {
        String sql = "SELECT event_payload FROM " + context.table("workflow_event") + " ORDER BY event_id ASC";
        try (Connection connection = context.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<GameEvent> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(GameEvent.valueOf(resultSet.getString(1)));
            }
            return List.copyOf(result);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read workflow events", e);
        }
    }
}
