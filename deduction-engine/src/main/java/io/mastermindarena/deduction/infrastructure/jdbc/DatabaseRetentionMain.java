package io.mastermindarena.deduction.infrastructure.jdbc;

/** Owner-only command: preview by default, deletion only with the exact --apply argument. */
public final class DatabaseRetentionMain {
    private DatabaseRetentionMain() {}

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !"--apply".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: DatabaseRetentionMain [--apply]");
        }
        try {
            var context = new JdbcPersistenceContext(required("APP_DB_URL"), required("APP_DB_USER"),
                    required("APP_DB_PASSWORD"), required("APP_DB_SCHEMA"));
            new JdbcSchemaMigrator(context).verifyLatest();
            var retention = new JdbcMatchRetention(context);
            var preview = retention.preview();
            System.out.println("Retention preview: cutoff=" + preview.cutoff() + ", expiredMatches=" + preview.expiredMatches());
            if (args.length == 1) System.out.println("Retention completed: deletedMatches=" + retention.purge());
        } catch (RuntimeException failure) {
            // Do not expose JDBC diagnostics, credentials or match contents through the CLI.
            throw new IllegalStateException("RETENTION_COMMAND_FAILED");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
