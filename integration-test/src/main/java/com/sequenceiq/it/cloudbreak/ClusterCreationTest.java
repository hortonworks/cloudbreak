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

import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.it.IntegrationTestContext;


public class ClusterCreationTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari" })
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("8080") String ambariPort, @Optional("admin") String ambariUser,
            @Optional("admin123!@#") String ambariPassword, @Optional("false") boolean emailNeeded,
            @Optional("false") boolean enableSecurity, @Optional String kerberosMasterKey, @Optional String kerberosAdmin, @Optional String kerberosPassword,
            @Optional("") String runRecipesOnHosts, @Optional("true") boolean checkAmbari) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        Set<HostGroupJson> hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword);
        // WHEN
        // TODO email needed
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setName(clusterName);
        clusterRequest.setDescription("Cluster for integration test");
        clusterRequest.setKerberosAdmin(kerberosAdmin);
        clusterRequest.setKerberosPassword(kerberosPassword);
        clusterRequest.setKerberosMasterKey(kerberosMasterKey);
        clusterRequest.setEnableSecurity(enableSecurity);
        clusterRequest.setPassword(ambariPassword);
        clusterRequest.setUserName(ambariUser);
        clusterRequest.setBlueprintId(Long.valueOf(blueprintId));
        clusterRequest.setHostGroups(hostGroupJsons1);

        ClusterEndpoint clusterEndpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterEndpoint.post(Long.valueOf(stackId), clusterRequest).getId();

        // THEN
        Assert.assertNotNull(clusterId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);
    }

    private Set<HostGroupJson> convertHostGroups(List<HostGroup> hostGroups, String runRecipesOnHosts) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        Set<HostGroupJson> hgMaps = new HashSet<>();
        for (HostGroup hostgroup : hostGroups) {
            HostGroupJson hostGroupJson = new HostGroupJson();
            hostGroupJson.setName(hostgroup.getName());
            ConstraintJson constraintJson = new ConstraintJson();
            constraintJson.setInstanceGroupName(hostgroup.getInstanceGroupName());
            constraintJson.setHostCount(hostgroup.getHostCount());
            hostGroupJson.setConstraint(constraintJson);
            if (hostGroupsWithRecipe.contains(hostgroup.getName())) {
                hostGroupJson.setRecipeIds(recipeIds);
            }
            hgMaps.add(hostGroupJson);
        }
        return hgMaps;
    }
}
