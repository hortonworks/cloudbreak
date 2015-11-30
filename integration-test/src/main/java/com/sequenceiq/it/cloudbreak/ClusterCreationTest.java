package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;

public class ClusterCreationTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "clusterName", "ambariUser", "ambariPassword", "emailNeeded",
            "enableSecurity", "kerberosMasterKey", "kerberosAdmin", "kerberosPassword",
            "runRecipesOnHosts" })
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("admin") String ambariUser,
            @Optional("admin123!@#") String ambariPassword, @Optional("false") boolean emailNeeded,
            @Optional("false") boolean enableSecurity, @Optional String kerberosMasterKey, @Optional String kerberosAdmin, @Optional String kerberosPassword,
            @Optional("") String runRecipesOnHosts) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        List<Map<String, Object>> map = convertHostGroups(hostgroups, runRecipesOnHosts);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword);
        // WHEN
        // TODO email needed
        CloudbreakClient client = getClient();
        client.postCluster(clusterName, ambariUser, ambariPassword, blueprintId, "Cluster for integration test", Integer.valueOf(stackId), map,
                enableSecurity, kerberosMasterKey, kerberosAdmin, kerberosPassword);
        // THEN
        CloudbreakUtil.waitAndCheckStackStatus(itContext, stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(client, stackIdStr, ambariUser, ambariPassword);
    }

    private List<Map<String, Object>> convertHostGroups(List<HostGroup> hostGroups, String runRecipesOnHosts) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        List<Map<String, Object>> hgMaps = new ArrayList<>();
        for (HostGroup hostgroup : hostGroups) {
            Map<String, Object> hgMap = new HashMap<>();
            hgMap.put("name", hostgroup.getName());
            hgMap.put("instanceGroupName", hostgroup.getInstanceGroupName());
            if (hostGroupsWithRecipe.contains(hostgroup.getName())) {
                hgMap.put("recipeIds", recipeIds);
            }
            hgMaps.add(hgMap);
        }
        return hgMaps;
    }
}
