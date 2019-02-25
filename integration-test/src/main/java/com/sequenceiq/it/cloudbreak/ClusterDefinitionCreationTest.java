package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.it.util.ResourceUtil;

public class ClusterDefinitionCreationTest extends AbstractCloudbreakIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Parameters({ "clusterDefinitionName", "clusterDefinitionFile" })
    public void testClusterDefinitionCreation(@Optional("it-hdp-multi-cluster-definition") String clusterDefinitionName,
            @Optional("classpath:/clusterdefinition/hdp-multinode-default.bp") String clusterDefinitionFile) throws Exception {
        // GIVEN
        String clusterDefinitionContent = ResourceUtil.readStringFromResource(applicationContext, clusterDefinitionFile);
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        // WHEN
        ClusterDefinitionV4Request clusterDefinitionRequest = new ClusterDefinitionV4Request();
        clusterDefinitionRequest.setName(clusterDefinitionName);
        clusterDefinitionRequest.setDescription("Cluster definition for integration testing");
        clusterDefinitionRequest.setClusterDefinition(clusterDefinitionContent);
        String id = getCloudbreakClient().clusterDefinitionV4Endpoint().post(workspaceId, clusterDefinitionRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CLUSTER_DEFINITION_ID, id, true);
    }
}
