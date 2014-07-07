package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.domain.SnsRequest;

@Service
public class SnsMessageParser {

    public SnsRequest parseRequest(String request) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(request, SnsRequest.class);
    }

    public Map<String, String> parseCFMessage(String message) {
        Map<String, String> result = new HashMap<>();
        String[] entries = message.split("\n");
        for (String entry : entries) {
            String key = entry.substring(0, entry.indexOf("="));
            String value = entry.substring(entry.indexOf("=") + 2, entry.length() - 1);
            result.put(key, value);
        }
        return result;

    }
}
