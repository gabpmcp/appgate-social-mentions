package org.appgate.services;

import java.util.function.Consumer;

public class DbService {
    private final String host;
    private final int port;

    public DbService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Inserting Facebook post using HOF
    public Consumer<DbOperation> insertFBPost() {
        return operation -> {
            // Aquí puedes implementar la lógica de interacción con tu base de datos actual (PostgreSQL, MongoDB, etc.)
            System.out.println("Inserting into FB table: " + operation.table + " with score " + operation.score);
            // Código real de inserción aquí
        };
    }

    // Inserting Tweet using HOF
    public Consumer<DbOperation> insertTweet() {
        return operation -> {
            // Aquí puedes implementar la lógica de interacción con tu base de datos actual
            System.out.println("Inserting into Tweets table: " + operation.table + " with score " + operation.score);
            // Código real de inserción aquí
        };
    }
}
