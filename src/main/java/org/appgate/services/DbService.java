package org.appgate.services;

import io.vavr.Function1;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.io.Serializable;

public class DbService {
    // Función para insertar Facebook posts (sin estado)
    public static Function1<Map<String, Serializable>, Map<String, Serializable>> insertFBPost(
            Function1<Map<String, Serializable>, Map<String, Serializable>> externalInteraction) {
        return externalInteraction;
    }

    // Función para insertar Tweets (sin estado)
    public static Function1<Map<String, Serializable>, Map<String, Serializable>> insertTweet(
            Function1<Map<String, Serializable>, Map<String, Serializable>> externalInteraction) {
        return externalInteraction;
    }
}
