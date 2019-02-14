package com.sequenceiq.cloudbreak.clusterdefinition.knox;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType.CORE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType.GATEWAY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.clusterdefinition.filesystem.ClusterDefinitionTestUtil;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class KnoxConfigProviderTest {

    private final KnoxConfigProvider configProvider = new KnoxConfigProvider();

    @Test
    public void extendBluePrintWithKnoxGatewayForMaster() throws IOException {
        String baseBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-with-knox.bp");
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-with-knox-result.bp");
        ClusterDefinition clusterDefinition = TestUtil.clusterDefinition("name", baseBlueprint);
        TemplatePreparationObject object = buildPreparationObjectWithGateway();

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(clusterDefinition.getClusterDefinitionText());
        String actualBlueprint = configProvider.customTextManipulation(object, b).asText();

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void specialConditionFalse() {
        TemplatePreparationObject object = Builder.builder()
                .withGeneralClusterConfigs(ClusterDefinitionTestUtil.generalClusterConfigs())
                .build();
        assertFalse(configProvider.specialCondition(object, ""));
    }

    @Test
    public void specialConditionTrue() {
        TemplatePreparationObject object = buildPreparationObjectWithGateway();
        assertTrue(configProvider.specialCondition(object, ""));
    }

    private TemplatePreparationObject buildPreparationObjectWithGateway() {
        GeneralClusterConfigs config = ClusterDefinitionTestUtil.generalClusterConfigs();
        Set<HostgroupView> hostGroupsView = new HashSet<>();
        HostgroupView hg1 = new HostgroupView("master", 0,  GATEWAY, 2);
        HostgroupView hg2 = new HostgroupView("slave_1", 0,  CORE, 2);
        HostgroupView hg3 = new HostgroupView("slave_2", 0,  GATEWAY, 2);
        hostGroupsView.add(hg1);
        hostGroupsView.add(hg2);
        hostGroupsView.add(hg3);
        config.setGatewayInstanceMetadataPresented(true);
        return Builder.builder()
                .withGeneralClusterConfigs(config)
                .withHostgroupViews(hostGroupsView)
                .build();
    }
}