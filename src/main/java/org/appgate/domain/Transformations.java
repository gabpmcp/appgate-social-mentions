package org.appgate.domain;

import io.vavr.Function1;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.appgate.util.Constants;
import org.appgate.util.Dependency;

import java.io.Serializable;

import static org.appgate.util.Utils.getValue;

public class Transformations {

    // Agrega flags para identificar si es Facebook o Tweeter
    public static Map<String, Serializable> addFacebookTwitterFlags(Map<String, Serializable> mentionData) {
        boolean isFacebook = getValue(mentionData, "facebookAccount", Option.none()).isDefined();
        boolean isTweeter = !isFacebook && getValue(mentionData, "tweeterAccount", Option.none()).isDefined();
        return mentionData.put("isFacebook", isFacebook).put("isTweeter", isTweeter);
    }

    // Construye el mensaje basado en si es Facebook o Tweeter
    public static Map<String, Serializable> buildMessage(Map<String, Serializable> mentionData) {
        boolean isFacebook = getValue(mentionData, "isFacebook", false);
        String message = isFacebook
                ? "facebookMessage: " + getValue(mentionData, "message", "") + " || comments: " + String.join(" ", getValue(mentionData, "facebookComments", List.empty()))
                : "tweeterMessage: " + getValue(mentionData, "message", "");
        return mentionData.put("message", message);
    }

    // Calcula ambos puntajes (Facebook y Twitter) usando mapas
    public static Map<String, Serializable> calculateScores(Map<String, Serializable> mentionData) {
        return calculateFacebookScore(mentionData).merge(calculateTweeterScore(mentionData));
    }

    // Calcular puntaje de Facebook basado en el mensaje, ahora usando mapas
    private static Map<String, Serializable> calculateFacebookScore(Map<String, Serializable> mentionData) {
        String message = getValue(mentionData, "message", "");
        double commentsScore = FacebookAnalyzer.calculateFacebookCommentsScore(
                message.substring(message.indexOf("comments:"))
        );
        double facebookScore = commentsScore < 50 ? Constants.FACEBOOK_SCORE_HIGH_RISK
                : FacebookAnalyzer.analyzePost(message, (String) getValue(mentionData, "facebookAccount", ""));
        return mentionData.put("facebookScore", facebookScore);
    }

    // Calcular puntaje de Twitter basado en el mensaje, ahora usando mapas
    private static Map<String, Serializable> calculateTweeterScore(Map<String, Serializable> mentionData) {
        String message = getValue(mentionData, "message", "");
        double tweeterScore = TweeterAnalyzer.analyzeTweet(
                message, getValue(mentionData, "tweeterUrl", ""),
                (String) getValue(mentionData, "tweeterAccount", "")
        );
        return mentionData.put("tweeterScore", tweeterScore);
    }

    public static String determineRiskLevel(boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore) {
        if (isFacebook) {
            return facebookScore == Constants.FACEBOOK_SCORE_HIGH_RISK ? "HIGH_RISK"
                    : (facebookScore < Constants.FACEBOOK_SCORE_MEDIUM_RISK ? "MEDIUM_RISK" : "LOW_RISK");
        } else if (isTweeter) {
            return tweeterScore >= Constants.TWEETER_SCORE_HIGH_RISK && tweeterScore <= -0.5 ? "HIGH_RISK"
                    : (tweeterScore < Constants.TWEETER_SCORE_MEDIUM_RISK ? "MEDIUM_RISK" : "LOW_RISK");
        }
        return "Error, Tweeter or Facebook account must be present";
    }

    // Determina el nivel de riesgo usando mapas
    public static Map<String, Serializable> calculateRiskLevel(Map<String, Serializable> mentionData) {
        boolean isFacebook = getValue(mentionData, "isFacebook", false);
        boolean isTweeter = getValue(mentionData, "isTweeter", false);

        double facebookScore = getValue(mentionData, "facebookScore", 0d);
        double tweeterScore = getValue(mentionData, "tweeterScore", 0d);

        String riskLevel = determineRiskLevel(isFacebook, isTweeter, facebookScore, tweeterScore);
        return mentionData.put("riskLevel", riskLevel);
    }

    // Función genérica para crear operaciones de base de datos
    public static List<Map<String, Serializable>> addDbOperations(
            boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore,
            String message, Map<String, Serializable> mentionData) {

        List<Map<String, Serializable>> ops = List.empty();

        if (isFacebook) {
            ops = ops.append(createDbOperation(Constants.ANALYZED_FB_TABLE, message, getValue(mentionData, "facebookAccount", ""), facebookScore));
        }
        if (isTweeter) {
            ops = ops.append(createDbOperation(Constants.ANALYZED_TWEETS_TABLE, message, getValue(mentionData, "tweeterAccount", ""), tweeterScore));
        }

        return ops;
    }

    // Prepara las operaciones de la base de datos usando mapas
    public static Map<String, Serializable> prepareDbOperations(Map<String, Serializable> mentionData) {
        boolean isFacebook = getValue(mentionData, "isFacebook", false);
        boolean isTweeter = getValue(mentionData, "isTweeter", false);

        double facebookScore = getValue(mentionData, "facebookScore", 0d);
        double tweeterScore = getValue(mentionData, "tweeterScore", 0d);

        String message = getValue(mentionData, "message", "");

        List<Map<String, Serializable>> dbOperations = Transformations.addDbOperations(isFacebook, isTweeter, facebookScore, tweeterScore, message, mentionData);

        return mentionData.put("dbOperations", dbOperations);
    }

    // Función genérica para construir las operaciones de base de datos
    public static Map<String, Serializable> createDbOperation(String table, String message, String account, double score) {
        return HashMap.of(
                "table", table,
                "message", message,
                "account", account,
                "score", score
        );
    }

    // Función para persistir los eventos generados
    public static Function1<Function2<List<Map<String, Serializable>>, String, List<Map<String, Serializable>>>, Dependency> persistOperation = saveOperation -> result -> {
        //TODO: Persistence logic
        var events = getValue(result, "events", List.<Map<String, Serializable>>empty());
        var aggregateId = getValue(events.get(),"aggregateId", "");

        if(getValue(result, "command", HashMap.empty()).contains(Tuple.of("type", "CreateFranchise"))) {
            Function1<String, Map<String, Serializable>> createAggregateFunc =
                    getValue(result, "command.createAggregateFunc", null);
            createAggregateFunc.apply(aggregateId);
        }

        saveOperation.apply(events, aggregateId);
        return result.remove("command");
    };
}
