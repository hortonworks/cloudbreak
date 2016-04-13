package com.sequenceiq.cloudbreak.shell.transformer;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ExceptionTransformer {

    @Inject
    private ObjectMapper objectMapper;

    public RuntimeException transformToRuntimeException(Exception e) {
        if (e instanceof ClientErrorException) {
            if (((ClientErrorException) e).getResponse() != null && ((ClientErrorException) e).getResponse().hasEntity()) {
                String response = ((ClientErrorException) e).getResponse().readEntity(String.class);
                try {
                    JsonNode message = objectMapper.readTree(response).get("message");
                    return new RuntimeException(message.asText());
                } catch (IOException ex) {
                    return new RuntimeException(response);
                }
            }
        }
        return new RuntimeException(e.getMessage());
    }

    public RuntimeException transformToRuntimeException(String errorMessage) {
        return new RuntimeException(errorMessage);
    }
}
