package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient;

public class StackPostStrategy implements Strategy {
    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client = getTestContextCloudbreakClient().apply(integrationTestContext);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && stackEntity.getRequest().getCredentialName() == null) {
            stackEntity.getRequest().setCredentialName(credential.getName());
        }

        Cluster cluster = Cluster.getTestContextCluster().apply(integrationTestContext);
        if (cluster != null && stackEntity.getRequest().getClusterRequest() == null) {
            stackEntity.getRequest().setClusterRequest(cluster.getRequest());
        }

        stackEntity.setResponse(
                client.getCloudbreakClient()
                        .stackV2Endpoint()
                        .postPrivate(stackEntity.getRequest()));
    }
}
