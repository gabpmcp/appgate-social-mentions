package org.appgate.util;

import io.vavr.collection.Map;
import java.io.Serializable;
import java.util.function.Function;

public interface Dependency extends Function<Map<String, Serializable>, Map<String, Serializable>> {
}
