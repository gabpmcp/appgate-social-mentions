package org.appgate;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.appgate.domain.Transformations;
import org.appgate.models.SocialMention;

import io.vavr.collection.List;
import org.appgate.util.Dependency;

import java.io.Serializable;

import static org.appgate.util.Constants.ANALYZED_FB_TABLE;
import static org.appgate.util.Utils.*;
import static org.appgate.util.Utils.getValue;

@Controller
public class CommandController {

    // Inicialización del mapa de dependencias para DbHandler
    private static final Map<String, Dependency> depsLoader = createDbLoader();

    @Post("/AnalyzeSocialMention")
    @Produces(MediaType.TEXT_PLAIN)
    public String analyze(@Body SocialMention socialMention) {
        // Lógica funcional pura
        Map<String, Serializable> result = analyzeSocialMention(mapSocialMention(socialMention));

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

    // Transforma SocialMention en un mapa dinámico
    public static Map<String, Serializable> mapSocialMention(SocialMention socialMention) {
        return HashMap.of(
                "message", socialMention.message(),
                "facebookAccount", Option.of(socialMention.facebookAccount()),
                "tweeterAccount", Option.of(socialMention.tweeterAccount()),
                "facebookComments", List.ofAll(socialMention.facebookComments())
        );
    }

    // Pipeline para procesar analyzeSocialMention
    public static Map<String, Serializable> analyzeSocialMention(Map<String, Serializable> mentionData) {
        return mentionData
            .transform(Transformations::addFacebookTwitterFlags)  // Paso 1: Agregar flags
            .transform(Transformations::buildMessage)             // Paso 2: Construir el mensaje
            .transform(Transformations::calculateScores)          // Paso 3: Calcular puntajes
            .transform(Transformations::calculateRiskLevel)       // Paso 4: Determinar nivel de riesgo
            .transform(Transformations::prepareDbOperations)
            .transform(m -> depsLoader.get("insertTweet").get().apply(m));     // Paso 5: Preparar operaciones DB
    }
}
