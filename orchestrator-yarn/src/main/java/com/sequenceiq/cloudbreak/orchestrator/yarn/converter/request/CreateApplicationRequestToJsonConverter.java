package com.sequenceiq.cloudbreak.orchestrator.yarn.converter.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;

public class CreateApplicationRequestToJsonConverter {

    public String convert(CreateApplicationRequest createApplicationRequest) throws JsonProcessingException {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return objectWriter.writeValueAsString(createApplicationRequest);
    }

}
