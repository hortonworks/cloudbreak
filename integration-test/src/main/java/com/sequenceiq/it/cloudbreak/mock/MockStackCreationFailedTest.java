package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.DOCKER_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SWARM_API_ROOT;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.sequenceiq.it.spark.consul.ConsulMemberResponse;
import com.sequenceiq.it.spark.docker.model.Info;
import com.sequenceiq.it.spark.docker.model.InspectContainerResponse;

public class MockStackCreationFailedTest extends AbstractMockIntegrationTest {

    @BeforeMethod
    public void setContextParams() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class), "Template id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID), "Network id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID), "Security group id is mandatory.");
    }

    @Test
    @Parameters({ "stackName", "region", "onFailureAction", "threshold", "adjustmentType", "variant", "availabilityZone", "persistentStorage", "orchestrator",
            "mockPort" })
    public void testStackCreation(@Optional("testing1") String stackName, @Optional("europe-west1") String region,
            @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold, @Optional("EXACT") String adjustmentType,
            @Optional("")String variant, @Optional() String availabilityZone, @Optional() String persistentStorage, @Optional("SWARM") String orchestrator,
            @Optional("false") boolean useMockServer, @Optional("443") int mockPort) throws Exception {
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

        int numberOfServers = getNumberOfServers(instanceGroups);

        port(mockPort);
        addMockEndpoints(numberOfServers);
        initSpark();

        // WHEN
        String stackId = getCloudbreakClient().stackEndpoint().postPrivate(stackRequest).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "CREATE_FAILED");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId);
    }

    private void addMockEndpoints(int numberOfServers) {
        get(DOCKER_API_ROOT + "/info", (req, res) -> "");
        get(DOCKER_API_ROOT + "/containers/:container/json", (req, res) -> new InspectContainerResponse("id"), gson()::toJson);
        post(DOCKER_API_ROOT + "/containers/:container/start", (req, res) -> "");
        get(SWARM_API_ROOT + "/info", (req, res) -> new Info(numberOfServers), gson()::toJson);
        oneConsulMemberFailedToStart(numberOfServers);
    }

    private void oneConsulMemberFailedToStart(int numberOfServers) {
        get(CONSUL_API_ROOT + "/agent/members", new ConsulMemberResponse(numberOfServers), gson()::toJson);
    }

    private int getNumberOfServers(List<InstanceGroup> instanceGroups) {
        int numberOfServers = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            numberOfServers += instanceGroup.getNodeCount();
        }
        return numberOfServers;
    }
}
