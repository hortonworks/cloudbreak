package com.sequenceiq.cloudbreak.cmtemplate.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
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
            assertTrue(description.asText().contains(descriptionSubStr));
        } catch (NoSuchFileException noSuchFileException) {

        }
    }

    @Test
    public void isEnterpriseDatalakeTestwithTemplatePreparationObject() throws IOException {
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getBlueprintText()).thenReturn(FileReaderUtils.readFileFromPath(Path.of(
                String.format("../core/src/main/resources/defaults/blueprints/%s/%s.bp", "7.2.17", "cdp-sdx-enterprise"))));
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withBlueprintView(blueprintView)
                .build();

        assertTrue(underTest.isEnterpriseDatalake(tpo));


        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster("ENTERPRISE"));
        assertTrue(underTest.isEnterpriseDatalake(stack));

        stack.setCluster(createSdxCluster("MEDIUM_DUTY_HA"));
        assertFalse(underTest.isEnterpriseDatalake(stack));
    }

    @Test
    public void isEnterpriseDatalakeTestwithShape() throws IOException {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(createSdxCluster("ENTERPRISE"));
        assertTrue(underTest.isEnterpriseDatalake(stack));

        stack.setCluster(createSdxCluster("MEDIUM_DUTY_HA"));
        assertFalse(underTest.isEnterpriseDatalake(stack));
    }

    @Test
    public void isMediumDatalakeTest() throws IOException {
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getBlueprintText()).thenReturn(FileReaderUtils.readFileFromPath(Path.of(
                String.format("../core/src/main/resources/defaults/blueprints/%s/%s.bp", "7.2.17", "cdp-sdx-medium-ha"))));
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withBlueprintView(blueprintView)
                .build();

        assertFalse(underTest.isEnterpriseDatalake(tpo));
    }

    private Cluster createSdxCluster(String shape) throws IOException {
        String template = null;
        Blueprint blueprint = new Blueprint();
        switch (shape) {
            case "LIGHT_DUTY":
                template = "cdp-sdx";
                blueprint.setDescription("7.2.17 - SDX template with Atlas, HMS, Ranger and other services they are dependent on");
                break;
            case "MEDIUM_DUTY_HA":
                template = "cdp-sdx-medium-ha";
                blueprint.setDescription(".2.17 - Medium SDX template with Atlas, HMS, Ranger and other services they are dependent on." +
                        "  Services like HDFS, HBASE, RANGER, HMS have HA");
                break;
            case "ENTERPRISE":
                template = "cdp-sdx-enterprise";
                blueprint.setDescription(".2.17 - Enterprise SDX template with Atlas, HMS, Ranger and other services they are dependent on. " +
                        " Services like HDFS, HBASE, RANGER, HMS have HA");
                break;
            case "MICRO_DUTY":
                template = "cdp-sdx-micro";
                blueprint.setDescription("7.2.17 - Micro SDX template with Atlas, HMS, Ranger and other services they are dependent on");
                break;
            default:
                template = "cdp-sdx";
        }
        blueprint.setBlueprintText(
                FileReaderUtils.readFileFromPath(
                        Path.of(
                                String.format("../core/src/main/resources/defaults/blueprints/7.2.17/%s.bp", template))));

        Cluster sdxCluster = new Cluster();
        sdxCluster.setBlueprint(blueprint);
        return sdxCluster;
    }
}