package com.sequenceiq.cloudbreak.shell.support;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonRenderer {

    private final ObjectMapper objectMapper;

    public JsonRenderer() {
        this.objectMapper = new ObjectMapper();
    }

    public String render(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
}
