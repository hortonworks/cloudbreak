package com.sequenceiq.cloudbreak.cmtemplate.util;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BlueprintUtilsTest {

    @Spy
    private JsonHelper jsonHelper;

    @InjectMocks
    private BlueprintUtils underTest;

    @Test
    public void descriptionTest() throws IOException {
        validateDescription("7.2.17", "cdp-sdx-medium-ha", "Medium SDX template");
        validateDescription("7.2.17", "cdp-sdx-enterprise", "Enterprise SDX template");
        validateDescription("7.2.18", "cdp-sdx-medium-ha", "Medium SDX template");
        validateDescription("7.2.18", "cdp-sdx-enterprise", "Enterprise SDX template");
        validateDescription("7.2.19", "cdp-sdx-medium-ha", "Medium SDX template");
        validateDescription("7.2.19", "cdp-sdx-enterprise", "Enterprise SDX template");
        validateDescription("7.2.20", "cdp-sdx-medium-ha", "Medium SDX template");
        validateDescription("7.2.20", "cdp-sdx-enterprise", "Enterprise SDX template");
    }

    private void validateDescription(String version, String template, String descriptionSubStr) throws IOException {
        try {
            JsonNode jsonNode = underTest.convertStringToJsonNode(
                    FileReaderUtils.readFileFromPath(Path.of(
                            String.format("../core/src/main/resources/defaults/blueprints/%s/%s.bp", version, template))));
            JsonNode description = jsonNode.get("description");
            Assert.assertTrue(description.asText().contains(descriptionSubStr));
        } catch (NoSuchFileException noSuchFileException) {

        }
    }
}