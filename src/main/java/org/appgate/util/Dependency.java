package org.appgate.util;

import io.vavr.collection.Map;
import java.io.Serializable;

@FunctionalInterface
public interface Dependency {
    Map<String, Serializable> apply(Map<String, Serializable> input);
}