package io.mastermindarena.deduction.api.submitaction;

public final class LocalSubmitActionHttpServerMain {
    private LocalSubmitActionHttpServerMain() {
    }

    public static void main(String[] args) {
        LocalSubmitActionRuntimeConfig config = LocalSubmitActionRuntimeConfig.fromEnvironment();
        LocalSubmitActionHttpServer server = LocalSubmitActionHttpServer.create(config);
        server.start();

        System.out.println("SubmitAction local HTTP server started");
        System.out.println("Path: " + config.path());
        System.out.println("Port: " + server.port());
        System.out.println("Seed matchId: " + config.seedMatchId());
        System.out.println("Seed actorId: " + config.seedActorId());
    }
}
