package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.DOCKER_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SWARM_API_ROOT;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.InstanceGroup;
import com.sequenceiq.it.spark.consul.ConsulMemberResponse;
import com.sequenceiq.it.spark.docker.model.Info;
import com.sequenceiq.it.spark.docker.model.InspectContainerResponse;
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;

public class MockStackCreationWithSwarmSuccessTest extends AbstractMockIntegrationTest {

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
        List<InstanceGroupRequest> igMap = new ArrayList<>();
        for (InstanceGroup ig : instanceGroups) {
            InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
            instanceGroupRequest.setGroup(ig.getName());
            instanceGroupRequest.setNodeCount(ig.getNodeCount());
            instanceGroupRequest.setTemplateId(Long.valueOf(ig.getTemplateId()));
            instanceGroupRequest.setType(InstanceGroupType.valueOf(ig.getType()));
            igMap.add(instanceGroupRequest);
        }
        String credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID);
        String networkId = itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID);
        String securityGroupId = itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setName(stackName);
        stackRequest.setCredentialId(Long.valueOf(credentialId));
        stackRequest.setRegion(region);
        stackRequest.setOnFailureAction(OnFailureAction.valueOf(onFailureAction));
        FailurePolicyRequest failurePolicyRequest = new FailurePolicyRequest();
        failurePolicyRequest.setAdjustmentType(AdjustmentType.valueOf(adjustmentType));
        failurePolicyRequest.setThreshold(threshold);
        stackRequest.setFailurePolicy(failurePolicyRequest);
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

        int numberOfServers = getNumberOfServers(instanceGroups);

        port(mockPort);
        addSPIEndpoints(sshPort);
        addMockEndpoints(numberOfServers);
        initSpark();

        // WHEN
        String stackId = getCloudbreakClient().stackEndpoint().postPrivate(stackRequest).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId);

        verify(DOCKER_API_ROOT + "/info", "GET").atLeast(1).verify();
    }

    private void addSPIEndpoints(int sshPort) {
        post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(mockServerAddress, sshPort), gson()::toJson);
    }

    private void addMockEndpoints(int numberOfServers) {
        get(DOCKER_API_ROOT + "/info", (req, res) -> "");
        get(DOCKER_API_ROOT + "/containers/:container/json", "application/json", (req, res) -> new InspectContainerResponse("id"), gson()::toJson);
        post(DOCKER_API_ROOT + "/containers/:container/start", "application/json", (req, res) -> "");
        get(SWARM_API_ROOT + "/info", "application/json", (req, res) -> new Info(numberOfServers), gson()::toJson);
        get(CONSUL_API_ROOT + "/agent/members", "application/json", new ConsulMemberResponse(numberOfServers), gson()::toJson);
    }

    private int getNumberOfServers(List<InstanceGroup> instanceGroups) {
        int numberOfServers = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            numberOfServers += instanceGroup.getNodeCount();
        }
        return numberOfServers;
    }

}
