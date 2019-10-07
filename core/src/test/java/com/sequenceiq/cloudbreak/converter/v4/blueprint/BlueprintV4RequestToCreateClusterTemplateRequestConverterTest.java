package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.cdp.datahub.model.CreateClusterTemplateRequest;
import com.cloudera.cdp.datahub.model.DatahubResourceTagRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;

class BlueprintV4RequestToCreateClusterTemplateRequestConverterTest {

    private BlueprintV4RequestToCreateClusterTemplateRequestConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new BlueprintV4RequestToCreateClusterTemplateRequestConverter();
    }

    @Test
    void convert() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint("blueprint");
        request.setName("name");
        request.setDescription("desc");
        request.setTags(Map.of("k1", "v1", "k2", "v2"));
        CreateClusterTemplateRequest result = underTest.convert(request);
        assertEquals(request.getBlueprint(), result.getClusterTemplateContent());
        assertEquals(request.getName(), result.getClusterTemplateName());
        assertEquals(request.getDescription(), result.getDescription());
        List<DatahubResourceTagRequest> resultTags = result.getTags();
        request.getTags().forEach((key, value) -> {
            Optional<DatahubResourceTagRequest> first = resultTags.stream().filter(t -> key.equals(t.getKey())).findFirst();
            assertTrue(first.isPresent());
            assertEquals(value, first.get().getValue());
        });
    }
}
