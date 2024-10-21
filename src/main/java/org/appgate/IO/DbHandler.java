package org.appgate.IO;

import io.vavr.Function2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.appgate.util.Dependency;

import java.io.Serializable;

public class DbHandler {
    // Función para insertar Facebook posts (sin estado) usando la interfaz Dependency
    public static Dependency insertFBPost(Dependency externalInteraction) {
        return externalInteraction;
    }

    // Función para insertar Tweets (sin estado) usando la interfaz Dependency
    public static Function2<List<Map<String, Serializable>>, String, List<Map<String, Serializable>>> insertTweetDumb() {
        return (tweet, userId) -> {
            //TODO: Here should be the insert tweet logic (IO interaction)
            return List.empty();
        };
    }


}
