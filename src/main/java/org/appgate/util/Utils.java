package org.appgate.util;

import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.collection.HashMap;
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
}

