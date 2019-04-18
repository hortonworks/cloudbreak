package com.sequenceiq.caas.util;

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.caas.model.AltusKey;

public class CrnHelper {

    private CrnHelper() {

    }

    public static String extractCrnFromAuthHeader(String authHeader) {
        String authFirstPart = authHeader.substring(0, authHeader.indexOf("."));
        byte[] authMetaJson = Base64.getUrlDecoder().decode(authFirstPart);
        String resultCrn = "";
        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode root = om.readTree(authMetaJson);
            resultCrn = root.get("access_key_id").asText();
        } catch (IOException e) {
            throw new RuntimeException("authentication failure");
        }
        return new String(Base64.getUrlDecoder().decode(resultCrn));
    }

    public static String generateCrn(String tenant, String user) {
        return "crn:altus:iam:us-west-1:" + tenant + ":user:" + user;
    }

    public static AltusKey generateAltusApiKey(String tenant, String user) {
        return new AltusKey(Base64.getEncoder().encodeToString(generateCrn(tenant, user).getBytes()));

    }
}
