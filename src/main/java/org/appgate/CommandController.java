package org.appgate;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.appgate.models.SocialMention;
import org.appgate.services.DbService;

import io.vavr.collection.List;

@Controller
public class CommandController {
    public static final String ANALYZED_TWEETS_TABLE = "analyzed_tweets";
    public static final String ANALYZED_FB_TABLE = "analyzed_fb_posts";
    private final DbService dbService = new DbService();

    @Post("/AnalyzeSocialMention")
    @Produces(MediaType.TEXT_PLAIN)
    public String analyze(@Body SocialMention socialMention) {
        // Lógica funcional pura
        Tuple2<String, List<Tuple2<Map<String, Object>, Double>>> result = analyzeSocialMention(socialMention);

        // Shell imperativa para I/O
        result._2.forEach(op -> {
            if (op._1.get("table").equals(ANALYZED_FB_TABLE)) {
                dbService.insertFBPost().accept(op._1, op._2);  // HOF para FB
            } else {
                dbService.insertTweet().accept(op._1, op._2);    // HOF para Tweet
            }
        });

        return result._1;
    }

    // Función pura para analizar la mención social
    private Tuple2<String, List<Tuple2<Map<String, Object>, Double>>> analyzeSocialMention(SocialMention socialMention) {
        boolean isFacebook = socialMention.facebookAccount() != null;
        boolean isTweeter = !isFacebook && socialMention.tweeterAccount() != null;

        String message = buildMessage(socialMention, isFacebook);

        double facebookScore = isFacebook ? calculateFacebookScore(message, socialMention) : 0;
        double tweeterScore = isTweeter ? calculateTweeterScore(message, socialMention) : 0;

        String riskLevel = determineRiskLevel(isFacebook, isTweeter, facebookScore, tweeterScore);

        List<Tuple2<Map<String, Object>, Double>> dbOperations = prepareDbOperations(isFacebook, isTweeter, facebookScore, tweeterScore, message, socialMention);

        return Tuple.of(riskLevel, dbOperations);
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

    private List<Tuple2<Map<String, Object>, Double>> prepareDbOperations(
            boolean isFacebook, boolean isTweeter, double facebookScore, double tweeterScore,
            String message, SocialMention socialMention) {
        List<Tuple2<Map<String, Object>, Double>> ops = List.empty();
        if (isFacebook) {
            Map<String, Object> fbData = HashMap.of(
                    "table", ANALYZED_FB_TABLE,
                    "message", message,
                    "account", socialMention.facebookAccount()
            );
            ops = ops.append(Tuple.of(fbData, facebookScore));
        }
        if (isTweeter) {
            Map<String, Object> tweetData = HashMap.of(
                    "table", ANALYZED_TWEETS_TABLE,
                    "message", message,
                    "account", socialMention.tweeterAccount()
            );
            ops = ops.append(Tuple.of(tweetData, tweeterScore));
        }
        return ops;
    }
}
