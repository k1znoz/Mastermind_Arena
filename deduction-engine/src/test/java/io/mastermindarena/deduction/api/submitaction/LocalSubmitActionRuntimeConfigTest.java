package io.mastermindarena.deduction.api.submitaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalSubmitActionRuntimeConfigTest {
    @Test
    void convertsVercelPostgresUrlToJdbc() {
        assertEquals(
                "jdbc:postgresql://host.example:6543/postgres?sslmode=require",
                LocalSubmitActionRuntimeConfig.normalizeJdbcUrl(
                        "postgres://user:password@host.example:6543/postgres?sslmode=require"
                )
        );
    }

    @Test
    void removesEncodedCredentialsFromVercelPostgresUrl() {
        assertEquals(
                "jdbc:postgresql://pooler.example.com:6543/postgres?sslmode=require",
                LocalSubmitActionRuntimeConfig.normalizeJdbcUrl(
                        "postgres://postgres.project:p%40ssword@pooler.example.com:6543/postgres?sslmode=require"
                )
        );
    }

    @Test
    void preservesJdbcUrl() {
        String jdbcUrl = "jdbc:postgresql://host.example:5432/postgres?sslmode=require";
        assertEquals(jdbcUrl, LocalSubmitActionRuntimeConfig.normalizeJdbcUrl(jdbcUrl));
    }
}
