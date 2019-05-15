package com.sequenceiq.cloudbreak.json;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class JsonHelper {

    public JsonNode createJsonFromString(String jsonString) {
        try {
            return JsonUtil.readTree(jsonString);
        } catch (IOException e) {
            throw new CloudbreakApiException("Failed to parse JSON string.", e);
        }
    }

}
