package org.appgate.services;

import io.vavr.collection.Map;

import java.util.function.BiConsumer;

public class DbService {
    // Function for inserting a Facebook post (HOF with dynamic types using Vavr)
    public BiConsumer<Map<String, Object>, Double> insertFBPost() {
        return (data, score) -> {
            // Simulación de inserción en la base de datos
            System.out.println("Inserting FB post with data: " + data + " and score: " + score);
            // Lógica real de inserción
        };
    }

    // Function for inserting a Tweet (HOF with dynamic types using Vavr)
    public BiConsumer<Map<String, Object>, Double> insertTweet() {
        return (data, score) -> {
            // Simulación de inserción en la base de datos
            System.out.println("Inserting Tweet with data: " + data + " and score: " + score);
            // Lógica real de inserción
        };
    }
}
