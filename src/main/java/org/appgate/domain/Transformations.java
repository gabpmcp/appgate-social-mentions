package org.appgate.domain;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.appgate.models.SocialMention;
import org.appgate.util.Constants;

public class Transformations {

    public static String buildMessage(SocialMention socialMention, boolean isFacebook) {
        return (isFacebook ? "facebookMessage: " : "tweeterMessage: ") +
                socialMention.message() +
                (isFacebook ? " || comments: " + String.join(" ", socialMention.facebookComments()) : "");
    }

    public static double calculateFacebookScore(String message, SocialMention socialMention) {
        double commentsScore = FacebookAnalyzer.calculateFacebookCommentsScore(
                message.substring(message.indexOf("comments:"))
        );
        return commentsScore < 50 ? Constants.FACEBOOK_SCORE_HIGH_RISK : FacebookAnalyzer.analyzePost(message, socialMention.facebookAccount());
    }

    public static double calculateTweeterScore(String message, SocialMention socialMention) {
        return TweeterAnalyzer.analyzeTweet(
                message, socialMention.tweeterUrl(), socialMention.tweeterAccount()
        );
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

    // Función genérica para crear operaciones de base de datos
    public static List<Map<String, Object>> prepareDbOperations(
            boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore,
            String message, SocialMention socialMention) {

        List<Map<String, Object>> ops = List.empty();

        if (isFacebook) {
            ops = ops.append(createDbOperation(Constants.ANALYZED_FB_TABLE, message, socialMention.facebookAccount(), facebookScore));
        }
        if (isTweeter) {
            ops = ops.append(createDbOperation(Constants.ANALYZED_TWEETS_TABLE, message, socialMention.tweeterAccount(), tweeterScore));
        }

        return ops;
    }

    // Función genérica para construir las operaciones de base de datos
    public static Map<String, Object> createDbOperation(String table, String message, String account, double score) {
        return HashMap.of(
                "table", table,
                "message", message,
                "account", account,
                "score", score
        );
    }
}
