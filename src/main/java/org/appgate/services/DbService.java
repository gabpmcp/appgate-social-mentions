package org.appgate.services;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DbService {
    // Function for inserting a Facebook post (HOF with primitive types)
    public BiConsumer<String, Double> insertFBPost() {
        return (message, score) -> {
            // Simulación de inserción en la base de datos
            System.out.println("Inserting FB post with message: " + message + " and score: " + score);
            // Código real de inserción aquí, usando primitivos
        };
    }

    // Function for inserting a Tweet (HOF with primitive types)
    public BiConsumer<String, Double> insertTweet() {
        return (message, score) -> {
            // Simulación de inserción en la base de datos
            System.out.println("Inserting Tweet with message: " + message + " and score: " + score);
            // Código real de inserción aquí, usando primitivos
        };
    }
}
