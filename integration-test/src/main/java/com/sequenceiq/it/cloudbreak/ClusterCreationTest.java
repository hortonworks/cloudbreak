package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;

public class ClusterCreationTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationTest.class);

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "clusterName", "emailNeeded" })
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("false") boolean emailNeeded) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        List<Map<String, Object>> map = convertHostGroups(hostgroups);
        // WHEN
        // TODO email needed
        CloudbreakClient client = getClient();
        client.postCluster(clusterName, blueprintId, "Cluster for integration test", Integer.valueOf(stackId), map);
        // THEN
        CloudbreakUtil.waitForStackStatus(itContext, stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(client, stackIdStr);
    }

    private List<Map<String, Object>> convertHostGroups(List<HostGroup> hostGroups) {
        List<Map<String, Object>> hgMaps = new ArrayList<>();
        for (HostGroup hostgroup : hostGroups) {
            Map<String, Object> hgMap = new HashMap<>();
            hgMap.put("name", hostgroup.getName());
            hgMap.put("instanceGroupName", hostgroup.getInstanceGroupName());
            hgMaps.add(hgMap);
        }
        return hgMaps;
    }
}
