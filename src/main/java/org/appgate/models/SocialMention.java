package org.appgate.models;

import java.util.List;

public record SocialMention (
    String message,
    String facebookAccount,
    String tweeterAccount,
    String creationDate,
    String tweeterUrl,
    List<String> facebookComments
) { }
