package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;
import static spark.Spark.get;
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
import org.springframework.http.HttpStatus;
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
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.InstanceGroup;
import com.sequenceiq.it.spark.consul.ConsulMemberResponse;
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;
import com.sequenceiq.it.util.ServerAddressGenerator;
import com.sequenceiq.it.verification.Verification;

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
        stackRequest.setSecurityGroupId(Long.valueOf(securityGroupId));
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
        int numberOfServers = getServerCount(instanceGroups);

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

        verifyCalls(numberOfServers);
    }

    private void addSPIEndpoints(int sshPort) {
        post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(mockServerAddress, sshPort), gson()::toJson);
    }

    private void addMockEndpoints(int numberOfServers) {
        get(SALT_BOOT_ROOT + "/health", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/salt/server/pillar", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/salt/action/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            genericResponses.setResponses(new ArrayList<>());
            return genericResponses;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/hostname/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            ArrayList<GenericResponse> responses = new ArrayList<>();
            new ServerAddressGenerator(numberOfServers).iterateOver(address -> {
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(address);
                genericResponse.setStatus("host-" + address.replace(".", "-"));
                responses.add(genericResponse);
            });
            genericResponses.setResponses(responses);
            return genericResponses;
        }, gson()::toJson);
        post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(numberOfServers));

        get(CONSUL_API_ROOT + "/agent/members", "application/json", new ConsulMemberResponse(numberOfServers), gson()::toJson);
    }

    private void verifyCalls(int numberOfServers) {
        verify(SALT_BOOT_ROOT + "/health", "GET").exactTimes(1).verify();
//        verify(SALT_BOOT_ROOT + "/salt/server/pillar", "POST").exactTimes(1).bodyContains("192.168.0.1").bodyContains("\"bootstrap_expect\":3").verify();
        Verification distributeVerification = verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").exactTimes(1);
        new ServerAddressGenerator(numberOfServers).iterateOver(address -> distributeVerification.bodyContains("address\":\"" + address));
        distributeVerification.verify();
    }

    private int getServerCount(List<InstanceGroup> instanceGroups) {
        int numberOfServers = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            numberOfServers += instanceGroup.getNodeCount();
        }
        return numberOfServers;
    }

}
