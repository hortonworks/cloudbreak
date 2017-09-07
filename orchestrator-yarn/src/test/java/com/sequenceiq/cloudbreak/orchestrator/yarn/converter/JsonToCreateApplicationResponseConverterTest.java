package com.sequenceiq.cloudbreak.orchestrator.yarn.converter;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.converter.response.JsonToCreateApplicationResponseConverter;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.CreateApplicationResponse;

public class JsonToCreateApplicationResponseConverterTest {

    private static final String JSON_RESPONSE = "{\n"
            + "  \"uri\": \"/services/v1/applications/demo-app\",\n"
            + "  \"state\": \"ACCEPTED\"\n"
            + '}';

    @Test
    public void testConvert() throws Exception {
        JsonToCreateApplicationResponseConverter jsonToCreateApplicationResponseConverter = new JsonToCreateApplicationResponseConverter();
        CreateApplicationResponse createApplicationResponse = jsonToCreateApplicationResponseConverter.convert(JSON_RESPONSE);
        assertTrue(createApplicationResponse != null);
    }
}