package org.appgate.services;

import org.appgate.util.Dependency;

public class DbService {
    // Función para insertar Facebook posts (sin estado) usando la interfaz Dependency
    public static Dependency insertFBPost(Dependency externalInteraction) {
        return externalInteraction;
    }

    // Función para insertar Tweets (sin estado) usando la interfaz Dependency
    public static Dependency insertTweet(Dependency externalInteraction) {
        return externalInteraction;
    }
}
