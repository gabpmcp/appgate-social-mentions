package org.appgate.util;

import io.vavr.Function1;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.collection.HashMap;
import org.appgate.services.DbService;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class Utils {

    @SuppressWarnings("unchecked")
    public static <K, T> T getValue(Map<K, ?> map, K key, T defaultValue) {
        // Si la key es un String con ".", buscamos en el path anidado
        if (key instanceof String && ((String) key).contains(".")) {
            return (T) getValueByPath((Map<String, Object>) map, key.toString()).getOrElse(defaultValue);
        }
        // Si no es anidado, buscamos el valor atómico
        var value = map.get(key).toOption();
        return value.isDefined() ? (T) value.get() : defaultValue;
    }

    private static Option<Object> getValueByPath(Map<String, Object> map, String path) {
        return Option.of(path.split("\\."))
                .flatMap(keys -> traverse(map, keys, 0));
    }

    @SuppressWarnings("unchecked")
    private static Option<Object> traverse(Map<String, Object> map, String[] keys, int index) {
        return index == keys.length
                ? Option.none()
                : map.get(keys[index])
                .flatMap(v -> (index == keys.length - 1)
                        ? Option.of(v)
                        : Option.of(v).flatMap(subMap -> (subMap instanceof HashMap<?,?> s) ? traverse((Map<String, Object>) s, keys, index + 1) : traverse(HashMap.ofAll((LinkedHashMap<String, Object>) subMap), keys, index + 1)));
    }

    // Función para crear el loader de dependencias, ahora usando funciones puras
    public static Map<String, Function1<Map<String, Serializable>, Map<String, Serializable>>> createDbLoader() {
        return HashMap.of(
                "insertFBPost", DbService.insertFBPost(map -> {
                    // Lógica pura para insertar en Facebook
                    System.out.println("Inserting into Facebook DB with data: " + map);
                    return map;
                }),
                "insertTweet", DbService.insertTweet(map -> {
                    // Lógica pura para insertar en Tweets
                    System.out.println("Inserting into Tweets DB with data: " + map);
                    return map;
                })
        );
    }
}

