package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;
import com.sequenceiq.it.cloudbreak.tags.TagsUtil;
import com.sequenceiq.it.util.ResourceUtil;

public class StackCreationTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.publicKeyFile}")
    private String defaultPublicKeyFile;

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
    "userDefinedTags", "publicKeyFile" })
    public void testStackCreation(@Optional("testing1") String stackName, @Optional("europe-west1") String region,
            @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold, @Optional("EXACT") String adjustmentType,
            @Optional("")String variant, @Optional String availabilityZone, @Optional String persistentStorage,  @Optional("SALT") String orchestrator,
            @Optional ("") String userDefinedTags, @Optional("")String publicKeyFile)
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
            instanceGroupRequest.setSecurityGroupId(Long.valueOf(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID)));
            igMap.add(instanceGroupRequest);
        }
        String credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID);
        String networkId = itContext.getContextParam(CloudbreakITContextConstants.NETWORK_ID);
        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        StackRequest stackRequest = new StackRequest();
        StackAuthenticationRequest stackAuthenticationRequest = new StackAuthenticationRequest();
        stackAuthenticationRequest.setPublicKey(publicKey);
        stackRequest.setStackAuthentication(stackAuthenticationRequest);
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

        if (!userDefinedTags.isEmpty()) {
            stackRequest.setUserDefinedTags(TagsUtil.getTagsToCheck(userDefinedTags));
        }

        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType(orchestrator);
        stackRequest.setOrchestrator(orchestratorRequest);

        Map<String, String> map = new HashMap<>();
        if (persistentStorage != null && !persistentStorage.isEmpty()) {
            map.put("persistentStorage", persistentStorage);
        }
        stackRequest.setParameters(map);

        // WHEN
        String stackId = getCloudbreakClient().stackV1Endpoint().postPrivate(stackRequest).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId);
        ScalingUtil.putInstanceCountToContext(itContext, stackId);
    }
}
