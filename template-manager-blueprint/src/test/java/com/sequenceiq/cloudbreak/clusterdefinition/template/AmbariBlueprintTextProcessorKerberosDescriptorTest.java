package com.sequenceiq.cloudbreak.clusterdefinition.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(Parameterized.class)
public class AmbariBlueprintTextProcessorKerberosDescriptorTest {

    private final AmbariBlueprintProcessorFactory underTest = new AmbariBlueprintProcessorFactory();

    private String bpPath;

    public AmbariBlueprintTextProcessorKerberosDescriptorTest(String bpPath) {
        this.bpPath = bpPath;
    }

    @Parameterized.Parameters(name = "[{index}] Test BP path: {0}")
    public static Object[] data() {
        return new Object[]{
                "blueprints-jackson/bp-kerberized-w-YARN-wo-security.bp",
                "blueprints-jackson/bp-kerberized-w-YARN-wo-kerberos_descriptor.bp",
                "blueprints-jackson/bp-kerberized-w-YARN-w-empty-services.bp",
                "blueprints-jackson/bp-kerberized-w-YARN-w-YARN-service-w-empty-configurations.bp",
                "blueprints-jackson/bp-kerberized-w-YARN-w-capacity-scheduler-another-value.bp"
        };
    }

    @Test
    public void testModifyStackVersionWithThreeTag() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath(bpPath);
        String kerberosDescriptorJson = FileReaderUtils.readFileFromClasspath("handlebar/configurations/yarn/global-kerberos-descriptor.json");
        String result = underTest.get(testBlueprint).addKerberosDescriptorEntryStringToBlueprint(kerberosDescriptorJson, false).asText();

        testBpHasEverything(result);
    }

    private void testBpHasEverything(String result) throws IOException {
        JsonNode bpNode = JsonUtil.readTree(result).path("Blueprints");
        JsonNode securityNode = bpNode.path("security");
        assertFalse(securityNode.isMissingNode());
        JsonNode kerberosDescriptorNode = securityNode.path("kerberos_descriptor");
        assertFalse(kerberosDescriptorNode.isMissingNode());
        JsonNode servicesNode = kerberosDescriptorNode.path("services");
        assertFalse(servicesNode.isMissingNode());
        JsonNode yarnConfigObject = servicesNode.get(0);
        assertFalse(yarnConfigObject.isMissingNode());
        assertEquals("YARN", yarnConfigObject.get("name").textValue());
        JsonNode configurations = yarnConfigObject.path("configurations");
        assertFalse(configurations.isMissingNode());
        JsonNode configNode = configurations.get(0);
        assertFalse(configNode.isMissingNode());
        JsonNode capacitySchedulerNode = configNode.get("capacity-scheduler");
        assertFalse(capacitySchedulerNode.isMissingNode());
        assertEquals("*", capacitySchedulerNode.get("yarn.scheduler.capacity.root.default.acl_submit_applications").textValue());
    }
}