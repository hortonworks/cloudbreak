package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupConstraintV4Request;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.it.IntegrationTestContext;

public class ClusterCreationTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    @Parameters({"clusterName", "emailNeeded", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "withRDSConfig", "autoRecoveryMode", "withFs"})
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("false") boolean emailNeeded,
            @Optional String kerberosMasterKey,
            @Optional String kerberosAdmin, @Optional String kerberosPassword,
            @Optional("") String runRecipesOnHosts, @Optional("true") boolean checkAmbari,
            @Optional ("false") boolean withRDSConfig, @Optional ("false") boolean autoRecoveryMode,
            @Optional ("false") boolean withFs) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        Set<HostGroupV4Request> hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts, autoRecoveryMode);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);

        // WHEN
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setName(clusterName);
        clusterRequest.setDescription("Cluster for integration test");
        clusterRequest.setPassword(ambariPassword);
        clusterRequest.setUserName(ambariUser);
        clusterRequest.setBlueprintId(Long.valueOf(blueprintId));
        clusterRequest.setHostGroups(hostGroupJsons1);

        if (withFs) {
            clusterRequest = setFileSystem(itContext, clusterRequest);
        }

        ClusterV1Endpoint clusterV1Endpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterV1Endpoint.post(Long.valueOf(stackId), clusterRequest).getId();
        // THEN
        Assert.assertNotNull(clusterId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackV1Endpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);
    }

    private Set<HostGroupV4Request> convertHostGroups(Collection<HostGroup> hostGroups, String runRecipesOnHosts, Boolean autoRecoveryMode) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        Set<HostGroupV4Request> hgMaps = new HashSet<>(hostGroups.size());
        for (HostGroup hostgroup : hostGroups) {
            HostGroupV4Request hostGroupBase = new HostGroupV4Request();
            hostGroupBase.setName(hostgroup.getName());
            if (Boolean.TRUE.equals(autoRecoveryMode)) {
                hostGroupBase.setRecoveryMode(RecoveryMode.AUTO);
            }
            HostGroupConstraintV4Request constraintJson = new HostGroupConstraintV4Request();
            constraintJson.setInstanceGroupName(hostgroup.getInstanceGroupName());
            constraintJson.setHostCount(hostgroup.getHostCount());
            hostGroupBase.setConstraint(constraintJson);
            if (hostGroupsWithRecipe.contains(hostgroup.getName())) {
                hostGroupBase.setRecipeIds(recipeIds);
            }
            hgMaps.add(hostGroupBase);
        }
        return hgMaps;
    }

    private ClusterRequest setFileSystem(IntegrationTestContext itContext, ClusterRequest clusterRequest) {
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.FSREQUEST, FileSystemRequest.class), "Filesystem was not configured");
        FileSystemRequest fsRequest = itContext.getContextParam(CloudbreakITContextConstants.FSREQUEST, FileSystemRequest.class);
        clusterRequest.setFileSystem(fsRequest);
        return clusterRequest;
    }
}
