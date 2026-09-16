package io.mastermindarena.deduction.api.submitaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalSubmitActionRuntimeConfigTest {
    @Test
    void convertsVercelPostgresUrlToJdbc() {
        assertEquals(
                "jdbc:postgresql://user:password@host.example:6543/postgres?sslmode=require",
                LocalSubmitActionRuntimeConfig.normalizeJdbcUrl(
                        "postgres://user:password@host.example:6543/postgres?sslmode=require"
                )
        );
    }

    @Test
    void preservesJdbcUrl() {
        String jdbcUrl = "jdbc:postgresql://host.example:5432/postgres?sslmode=require";
        assertEquals(jdbcUrl, LocalSubmitActionRuntimeConfig.normalizeJdbcUrl(jdbcUrl));
    }
}
