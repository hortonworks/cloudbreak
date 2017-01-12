package com.sequenceiq.cloudbreak.orchestrator.yarn.converter.response;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.CreateApplicationResponse;

public class JsonToCreateApplicationResponseConverter {

    public CreateApplicationResponse convert(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonResponse, CreateApplicationResponse.class);
    }

}
