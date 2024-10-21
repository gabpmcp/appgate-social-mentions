package org.appgate;

import io.github.cdimascio.dotenv.Dotenv;
import io.micronaut.runtime.Micronaut;

import java.util.stream.Collectors;

public class Application {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();  // Cargar las variables del archivo .env
        var result = dotenv.entries().stream().peek(entry -> System.setProperty(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        System.out.println(result);
        Micronaut.run(Application.class, args);
    }
}