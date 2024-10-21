package org.appgate.services;

import io.vavr.collection.Map;

import java.util.function.Consumer;

public class DbService {
    // Function for inserting a Facebook post (HOF with Map)
    public Consumer<Map<String, Object>> insertFBPost() {
        return data -> {
            System.out.println("Inserting FB post with data: " + data);
            // Lógica real de inserción aquí
        };
    }

    // Function for inserting a Tweet (HOF with Map)
    public Consumer<Map<String, Object>> insertTweet() {
        return data -> {
            System.out.println("Inserting Tweet with data: " + data);
            // Lógica real de inserción aquí
        };
    }
}
