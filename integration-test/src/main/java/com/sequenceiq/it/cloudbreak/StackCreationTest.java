package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
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
    public void testStackCreation(@Optional("it-cluster") String clusterName, @Optional("testing1") String stackName, @Optional("europe-west1") String region,
            @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold, @Optional("EXACT") String adjustmentType,
            @Optional("")String variant, @Optional String availabilityZone, @Optional String persistentStorage,  @Optional("SALT") String orchestrator,
            @Optional ("") String userDefinedTags, @Optional("")String publicKeyFile, @Optional ("false") boolean withFs,
            @Optional("") String runRecipesOnHosts, @Optional ("false") boolean autoRecoveryMode)
            throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();

        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);

        // WHEN
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setName(clusterName);
        AmbariV4Request ambariV4Request = new AmbariV4Request();
        ambariV4Request.setPassword(ambariPassword);
        ambariV4Request.setUserName(ambariUser);
        ambariV4Request.setClusterDefinitionName(itContext.getContextParam(CloudbreakITContextConstants.CLUSTER_DEFINITION_NAME));
        clusterRequest.setAmbari(ambariV4Request);

        if (withFs) {
            clusterRequest = setFileSystem(itContext, clusterRequest);
        }

        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        List<InstanceGroupV4Request> igMap = new ArrayList<>();
        for (InstanceGroup ig : instanceGroups) {
            InstanceGroupV4Request instanceGroupRequest = new InstanceGroupV4Request();
            instanceGroupRequest.setName(ig.getName());
            instanceGroupRequest.setNodeCount(ig.getNodeCount());
            instanceGroupRequest.setTemplate(new InstanceTemplateV4Request());
            instanceGroupRequest.setType(InstanceGroupType.valueOf(ig.getType()));
            var secGroup = new SecurityGroupV4Request();
            secGroup.setSecurityGroupIds(Set.of(itContext.getContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID)));
            instanceGroupRequest.setSecurityGroup(secGroup);
            igMap.add(instanceGroupRequest);
        }
        String credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_NAME);
        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        StackV4Request stackRequest = new StackV4Request();
        StackAuthenticationV4Request stackAuthenticationRequest = new StackAuthenticationV4Request();
        stackAuthenticationRequest.setPublicKey(publicKey);
        stackRequest.setAuthentication(stackAuthenticationRequest);
        stackRequest.setName(stackName);
        stackRequest.setCluster(clusterRequest);
        var environment = new EnvironmentSettingsV4Request();
        environment.setCredentialName(credentialId);
        var placement = new PlacementSettingsV4Request();
        placement.setRegion(region);
        placement.setAvailabilityZone(availabilityZone);
        stackRequest.setPlacement(placement);
        stackRequest.setEnvironment(environment);
        var network = new NetworkV4Request();
        stackRequest.setNetwork(network);
        stackRequest.setInstanceGroups(igMap);

        if (!userDefinedTags.isEmpty()) {
            stackRequest.getTags().setUserDefined(TagsUtil.getTagsToCheck(userDefinedTags));
        }

        // WHEN
        String stackId = getCloudbreakClient().stackV4Endpoint().post(workspaceId, stackRequest).getId().toString();
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackName);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackName);
        ScalingUtil.putInstanceCountToContext(itContext, workspaceId, stackName);
    }

    private ClusterV4Request setFileSystem(IntegrationTestContext itContext, ClusterV4Request clusterRequest) {
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.FSREQUEST, CloudStorageV4Request.class), "CloudStorage was not configured");
        CloudStorageV4Request fsRequest = itContext.getContextParam(CloudbreakITContextConstants.FSREQUEST, CloudStorageV4Request.class);
        clusterRequest.setCloudStorage(fsRequest);
        return clusterRequest;
    }

}
