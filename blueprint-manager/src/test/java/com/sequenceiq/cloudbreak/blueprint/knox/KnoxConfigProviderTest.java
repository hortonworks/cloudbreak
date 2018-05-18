package com.sequenceiq.cloudbreak.blueprint.knox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject.Builder;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class KnoxConfigProviderTest {

    private KnoxConfigProvider configProvider = new KnoxConfigProvider();

    @Test
    public void extendBluePrintWithKnoxGateway() throws IOException {
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-with-knox.bp");
        Blueprint blueprint = TestUtil.blueprint("name", expectedBlueprint);
        BlueprintPreparationObject object = buildPreparationObjectWithGateway();

        BlueprintTextProcessor b = new BlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = configProvider.customTextManipulation(object, b).asText();

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void specialConditionFalse() {
        BlueprintPreparationObject object = Builder.builder()
                .withGeneralClusterConfigs(BlueprintTestUtil.generalClusterConfigs())
                .build();
        assertFalse(configProvider.specialCondition(object, ""));
    }

    @Test
    public void specialConditionTrue() {
        BlueprintPreparationObject object = buildPreparationObjectWithGateway();
        assertTrue(configProvider.specialCondition(object, ""));
    }

    private BlueprintPreparationObject buildPreparationObjectWithGateway() {
        GeneralClusterConfigs config = BlueprintTestUtil.generalClusterConfigs();
        config.setGatewayInstanceMetadataPresented(true);
        return Builder.builder()
                .withGeneralClusterConfigs(config)
                .withHostgroups(TestUtil.hostGroups(Sets.newHashSet("master", "slave")))
                .build();
    }
}