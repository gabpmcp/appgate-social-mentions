package org.appgate;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import org.appgate.models.SocialMention;
import org.appgate.services.DbService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CommandController {
    public static final String ANALYZED_TWEETS_TABLE = "analyzed_tweets";
    public static final String ANALYZED_FB_TABLE = "analyzed_fb_posts";
    private final DbService dbService;

    public CommandController(DbService dbService) {
        this.dbService = dbService;
    }

    @Post("/AnalyzeSocialMention")
    @Produces(MediaType.TEXT_PLAIN)
    public String analyze(@Body SocialMention socialMention) {
        // Lógica funcional pura
        var result = analyzeSocialMention(socialMention);

        // Shell imperativa para I/O
        result.getDbOperations().forEach(op -> {
            if (op.table.equals(ANALYZED_FB_TABLE)) {
                dbService.insertFBPost().accept(op.message, op.score);  // HOF para FB
            } else {
                dbService.insertTweet().accept(op.message, op.score);    // HOF para Tweet
            }
        });

        return result.riskLevel;
    }

    // Función pura para analizar la mención social
    private AnalysisResult analyzeSocialMention(SocialMention socialMention) {
        boolean isFacebook = socialMention.facebookAccount() != null;
        boolean isTweeter = !isFacebook && socialMention.tweeterAccount() != null;

        String message = buildMessage(socialMention, isFacebook);

        double facebookScore = isFacebook ? calculateFacebookScore(message, socialMention) : 0;
        double tweeterScore = isTweeter ? calculateTweeterScore(message, socialMention) : 0;

        String riskLevel = determineRiskLevel(isFacebook, isTweeter, facebookScore, tweeterScore);

        List<DbOperation> dbOperations = prepareDbOperations(isFacebook, isTweeter, facebookScore, tweeterScore, message);

        return new AnalysisResult(riskLevel, dbOperations);
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
        return commentsScore < 50 ? -100 : FacebookAnalyzer.analyzePost(message, socialMention.facebookAccount());
    }

    private double calculateTweeterScore(String message, SocialMention socialMention) {
        return TweeterAnalyzer.analyzeTweet(
                message, socialMention.tweeterUrl(), socialMention.tweeterAccount()
        );
    }

    private String determineRiskLevel(boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore) {
        if (isFacebook) {
            return facebookScore == -100 ? "HIGH_RISK"
                    : (facebookScore < 50 ? "MEDIUM_RISK" : "LOW_RISK");
        } else if (isTweeter) {
            return tweeterScore >= -1 && tweeterScore <= -0.5 ? "HIGH_RISK"
                    : (tweeterScore < 0.7 ? "MEDIUM_RISK" : "LOW_RISK");
        }
        return "Error, Tweeter or Facebook account must be present";
    }

    private List<DbOperation> prepareDbOperations(boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore, String message) {
        List<DbOperation> ops = new ArrayList<>();
        if (isFacebook) {
            ops.add(new DbOperation(ANALYZED_FB_TABLE, facebookScore, message));
        }
        if (isTweeter) {
            ops.add(new DbOperation(ANALYZED_TWEETS_TABLE, tweeterScore, message));
        }
        return ops;
    }

    // Encapsulando el resultado del análisis
    static class AnalysisResult {
        String riskLevel;
        List<DbOperation> dbOperations;

        AnalysisResult(String riskLevel, List<DbOperation> dbOperations) {
            this.riskLevel = riskLevel;
            this.dbOperations = dbOperations;
        }

        List<DbOperation> getDbOperations() {
            return dbOperations;
        }
    }

    // Encapsulando las operaciones de base de datos como datos
    static class DbOperation {
        String table;
        double score;
        String message;

        DbOperation(String table, double score, String message) {
            this.table = table;
            this.score = score;
            this.message = message;
        }
    }
}
