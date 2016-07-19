package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static spark.Spark.port;
import static spark.Spark.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.InstanceGroup;
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;

public class MockStackCreationWithSaltSuccessTest extends AbstractMockIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockStackCreationWithSaltSuccessTest.class);

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Inject
    private ResourceLoader resourceLoader;

    @BeforeMethod
    public void setContextParams() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class), "Template id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "Network id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.");
    }

    @Test
    @Parameters({"stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "orchestrator",
            "mockPort", "sshPort"})
    public void testStackCreation(@Optional("testing1") String stackName, @Optional("europe-west1") String region,
            @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold, @Optional("EXACT") String adjustmentType,
            @Optional("") String variant, @Optional() String availabilityZone, @Optional() String persistentStorage, @Optional("SWARM") String orchestrator,
            @Optional("443") int mockPort, @Optional("2020") int sshPort)
            throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        List<InstanceGroupJson> igMap = new ArrayList<>();
        for (InstanceGroup ig : instanceGroups) {
            InstanceGroupJson instanceGroupJson = new InstanceGroupJson();
            instanceGroupJson.setGroup(ig.getName());
            instanceGroupJson.setNodeCount(ig.getNodeCount());
            instanceGroupJson.setTemplateId(Long.valueOf(ig.getTemplateId()));
            instanceGroupJson.setType(InstanceGroupType.valueOf(ig.getType()));
            igMap.add(instanceGroupJson);
        }
        String credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID);
        String networkId = itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID);
        String securityGroupId = itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setName(stackName);
        stackRequest.setCredentialId(Long.valueOf(credentialId));
        stackRequest.setRegion(region);
        stackRequest.setOnFailureAction(OnFailureAction.valueOf(onFailureAction));
        FailurePolicyJson failurePolicyJson = new FailurePolicyJson();
        failurePolicyJson.setAdjustmentType(AdjustmentType.valueOf(adjustmentType));
        failurePolicyJson.setThreshold(threshold);
        stackRequest.setFailurePolicy(failurePolicyJson);
        stackRequest.setNetworkId(Long.valueOf(networkId));
        stackRequest.setPlatformVariant(variant);
        stackRequest.setAvailabilityZone(availabilityZone);
        stackRequest.setInstanceGroups(igMap);

        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType(orchestrator);
        stackRequest.setOrchestrator(orchestratorRequest);

        Map<String, String> map = new HashMap<>();
        if (persistentStorage != null && !persistentStorage.isEmpty()) {
            map.put("persistentStorage", persistentStorage);
        }
        stackRequest.setParameters(map);
        port(mockPort);
        addSPIEndpoints(sshPort);
        initSpark();

        // WHEN
        String stackId = getCloudbreakClient().stackEndpoint().postPrivate(stackRequest).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId);
    }

    private void addSPIEndpoints(int sshPort) {
        post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(mockServerAddress, sshPort), gson()::toJson);
    }

}
