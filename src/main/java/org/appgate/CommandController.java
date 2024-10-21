package org.appgate;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.vavr.Function1;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.appgate.models.SocialMention;
import org.appgate.services.DbService;

import io.vavr.collection.List;
import org.appgate.util.Constants;

import java.io.Serializable;

import static org.appgate.util.Constants.ANALYZED_FB_TABLE;
import static org.appgate.util.Utils.*;
import static org.appgate.util.Utils.getValue;

@Controller
public class CommandController {

    // Inicialización del mapa de dependencias para DbService
    private final Map<String, Function1<Map<String, Serializable>, Map<String, Serializable>>> depsLoader = createDbLoader();
    private final DbService dbService = new DbService(depsLoader);

    @Post("/AnalyzeSocialMention")
    @Produces(MediaType.TEXT_PLAIN)
    public String analyze(@Body SocialMention socialMention) {
        // Lógica funcional pura
        Map<String, Object> result = analyzeSocialMention(socialMention);

        // Shell imperativa para I/O
        List<Map<String, Object>> dbOperations = getValue(result, "dbOperations", List.empty());
        dbOperations.forEach(op -> {
            String table = getValue(op, "table", "");
            Map<String, Serializable> operationData = HashMap.of(
                    "table", getValue(op, "table", ""),
                    "message", getValue(op, "message", ""),
                    "account", getValue(op, "account", "")
            );

            if (table.equals(ANALYZED_FB_TABLE)) {
                depsLoader.get("insertFBPost").get().apply(operationData);
            } else {
                depsLoader.get("insertTweet").get().apply(operationData);
            }
        });

        return getValue(result, "riskLevel", "Error");
    }

    // Función pura para analizar la mención social
    private Map<String, Object> analyzeSocialMention(SocialMention socialMention) {
        boolean isFacebook = socialMention.facebookAccount() != null;
        boolean isTweeter = !isFacebook && socialMention.tweeterAccount() != null;

        String message = buildMessage(socialMention, isFacebook);

        double facebookScore = isFacebook ? calculateFacebookScore(message, socialMention) : 0;
        double tweeterScore = isTweeter ? calculateTweeterScore(message, socialMention) : 0;

        String riskLevel = determineRiskLevel(isFacebook, isTweeter, facebookScore, tweeterScore);

        List<Map<String, Object>> dbOperations = prepareDbOperations(isFacebook, isTweeter, facebookScore, tweeterScore, message, socialMention);

        return HashMap.of(
                "riskLevel", riskLevel,
                "dbOperations", dbOperations
        );
    }

    private String buildMessage(SocialMention socialMention, boolean isFacebook) {
        return (isFacebook ? "facebookMessage: " : "tweeterMessage: ") +
                socialMention.message() +
                (isFacebook ? " || comments: " + String.join(" ", socialMention.facebookComments()) : "");
    }

    private double calculateFacebookScore(String message, SocialMention socialMention) {
        double commentsScore = FacebookAnalyzer.calculateFacebookCommentsScore(
                message.substring(message.indexOf("comments:"))
        );
        return commentsScore < 50 ? Constants.FACEBOOK_SCORE_HIGH_RISK : FacebookAnalyzer.analyzePost(message, socialMention.facebookAccount());
    }

    private double calculateTweeterScore(String message, SocialMention socialMention) {
        return TweeterAnalyzer.analyzeTweet(
                message, socialMention.tweeterUrl(), socialMention.tweeterAccount()
        );
    }

    private String determineRiskLevel(boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore) {
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
    private List<Map<String, Object>> prepareDbOperations(
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
    private Map<String, Object> createDbOperation(String table, String message, String account, double score) {
        return HashMap.of(
                "table", table,
                "message", message,
                "account", account,
                "score", score
        );
    }
}
