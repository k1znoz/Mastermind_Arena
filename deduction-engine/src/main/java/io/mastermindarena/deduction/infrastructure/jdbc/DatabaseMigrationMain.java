package io.mastermindarena.deduction.infrastructure.jdbc;

/** Explicit owner-only command. Credentials come from process environment, never arguments or logs. */
public final class DatabaseMigrationMain {
    private DatabaseMigrationMain() {}

    public static void main(String[] args) {
        var context = new JdbcPersistenceContext(required("APP_DB_URL"), required("APP_DB_USER"),
                required("APP_DB_PASSWORD"), required("APP_DB_SCHEMA"));
        var migrator = new JdbcSchemaMigrator(context);
        migrator.migrateToLatest();
        String role = System.getenv("APP_DB_RUNTIME_ROLE");
        if (role != null && !role.isBlank()) migrator.grantRuntimeAccess(role);
        System.out.println("Database migration completed");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
