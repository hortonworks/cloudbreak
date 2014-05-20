package com.sequenceiq.provisioning.controller.json;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.provisioning.controller.InternalServerException;

@Component
public class JsonHelper {

    public JsonNode createJsonFromString(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getFactory();
            JsonParser jp = factory.createParser(jsonString);
            JsonNode actualObj = mapper.readTree(jp);
            return actualObj;
        } catch (IOException e) {
            throw new InternalServerException("Failed to parse JSON string.");
        }
    }

}
