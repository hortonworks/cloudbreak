package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;
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
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
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
    @Parameters({"clusterName", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "withRDSConfig", "autoRecoveryMode", "withFs"})
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("false") boolean emailNeeded,
            @Optional("false") boolean enableSecurity, @Optional String kerberosMasterKey,
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
        Set<HostGroupRequest> hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts, autoRecoveryMode);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);

        // WHEN
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setName(clusterName);
        clusterRequest.setDescription("Cluster for integration test");
        clusterRequest.setEnableSecurity(enableSecurity);
        clusterRequest.setPassword(ambariPassword);
        clusterRequest.setUserName(ambariUser);
        clusterRequest.setBlueprintId(Long.valueOf(blueprintId));
        clusterRequest.setHostGroups(hostGroupJsons1);

        if (withRDSConfig) {
            clusterRequest = setRDSConfiguration(itContext, clusterRequest);
        }

        if (withFs) {
            clusterRequest = setFileSystem(itContext, clusterRequest);
        }

        KerberosRequest kerberosRequest = new KerberosRequest();
        kerberosRequest.setAdmin(kerberosAdmin);
        kerberosRequest.setPassword(kerberosPassword);
        kerberosRequest.setMasterKey(kerberosMasterKey);
        clusterRequest.setKerberos(kerberosRequest);

        ClusterV1Endpoint clusterV1Endpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterV1Endpoint.post(Long.valueOf(stackId), clusterRequest).getId();
        // THEN
        Assert.assertNotNull(clusterId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);

        if (Boolean.TRUE.equals(withRDSConfig)) {
            checkRDSConfigWithCluster(itContext, clusterName);
        }
    }

    private Set<HostGroupRequest> convertHostGroups(List<HostGroup> hostGroups, String runRecipesOnHosts, Boolean autoRecoveryMode) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        Set<HostGroupRequest> hgMaps = new HashSet<>(hostGroups.size());
        for (HostGroup hostgroup : hostGroups) {
            HostGroupRequest hostGroupBase = new HostGroupRequest();
            hostGroupBase.setName(hostgroup.getName());
            if (Boolean.TRUE.equals(autoRecoveryMode)) {
                hostGroupBase.setRecoveryMode(RecoveryMode.AUTO);
            }
            ConstraintJson constraintJson = new ConstraintJson();
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

    private ClusterRequest setRDSConfiguration(IntegrationTestContext itContext, ClusterRequest clusterRequest) {
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.RDS_CONFIG_ID), "RDS configuration id is missing.");
        long rdsConfigId = Long.parseLong(itContext.getContextParam(CloudbreakITContextConstants.RDS_CONFIG_ID));
        Set<Long> rdsConfigIds = new HashSet<>();
        rdsConfigIds.add(rdsConfigId);
        clusterRequest.setRdsConfigIds(rdsConfigIds);
        return clusterRequest;
    }

    private void checkRDSConfigWithCluster(IntegrationTestContext itContext, String clusterName) {
        boolean clusterIsFound = false;
        long rdsConfigId = Long.parseLong(itContext.getContextParam(CloudbreakITContextConstants.RDS_CONFIG_ID));
        Set<String> clusterNames = getCloudbreakClient().rdsConfigEndpoint().get(rdsConfigId).getClusterNames();
        for (String name : clusterNames) {
            if (name.equals(clusterName)) {
                clusterIsFound = true;
                break;
            }
        }
        Assert.assertTrue(clusterIsFound, "The RDS configuration is not connected to the cluster");
    }

    private ClusterRequest setFileSystem(IntegrationTestContext itContext, ClusterRequest clusterRequest) {
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.FSREQUEST, FileSystemRequest.class), "Filesystem was not configured");
        FileSystemRequest fsRequest = itContext.getContextParam(CloudbreakITContextConstants.FSREQUEST, FileSystemRequest.class);
        clusterRequest.setFileSystem(fsRequest);
        return clusterRequest;
    }
}
