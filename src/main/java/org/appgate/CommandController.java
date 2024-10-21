package org.appgate;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.vavr.Function1;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.appgate.domain.Transformations;
import org.appgate.models.SocialMention;
import org.appgate.services.DbService;

import io.vavr.collection.List;
import org.appgate.util.Constants;
import org.appgate.util.Dependency;

import java.io.Serializable;

import static org.appgate.util.Constants.ANALYZED_FB_TABLE;
import static org.appgate.util.Utils.*;
import static org.appgate.util.Utils.getValue;

@Controller
public class CommandController {

    // Inicialización del mapa de dependencias para DbService
    private final Map<String, Dependency> depsLoader = createDbLoader();
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

        String message = Transformations.buildMessage(socialMention, isFacebook);

        double facebookScore = isFacebook ? Transformations.calculateFacebookScore(message, socialMention) : 0;
        double tweeterScore = isTweeter ? Transformations.calculateTweeterScore(message, socialMention) : 0;

        String riskLevel = Transformations.determineRiskLevel(isFacebook, isTweeter, facebookScore, tweeterScore);

        List<Map<String, Object>> dbOperations = Transformations.prepareDbOperations(isFacebook, isTweeter, facebookScore, tweeterScore, message, socialMention);

        return HashMap.of(
                "riskLevel", riskLevel,
                "dbOperations", dbOperations
        );
    }
}
