package org.appgate.services;

import io.vavr.Function1;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.io.Serializable;

public class DbService {
    private final Map<String, Function1<Map<String, Serializable>, Map<String, Serializable>>> depsLoader;

    public DbService(Map<String, Function1<Map<String, Serializable>, Map<String, Serializable>>> depsLoader) {
        this.depsLoader = depsLoader;
    }

    // Function to insert Facebook posts (using dependencies)
    public Function1<Map<String, Serializable>, Map<String, Serializable>> insertFBPost() {
        return depsLoader.get("insertFBPost").getOrElse(HashMap.empty());
    }

    // Function to insert Tweets (using dependencies)
    public Function1<Map<String, Serializable>, Map<String, Serializable>> insertTweet() {
        return depsLoader.get("insertTweet").getOrElse(HashMap.empty());
    }
}
