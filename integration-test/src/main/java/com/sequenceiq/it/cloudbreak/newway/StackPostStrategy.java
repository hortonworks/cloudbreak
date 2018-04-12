package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

public class StackPostStrategy implements Strategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackPostStrategy.class);

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client = getTestContextCloudbreakClient().apply(integrationTestContext);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);

        if (credential != null && stackEntity.getRequest().getGeneral().getCredentialName() == null) {
            stackEntity.getRequest().getGeneral().setCredentialName(credential.getName());
        }

        Cluster cluster = Cluster.getTestContextCluster().apply(integrationTestContext);
        if (cluster != null && stackEntity.getRequest().getCluster() == null) {
            stackEntity.getRequest().setCluster(cluster.getRequest());
        }

        ImageSettings imageSettings = ImageSettings.getTestContextImageSettings().apply(integrationTestContext);
        if (imageSettings != null) {
            stackEntity.getRequest().setImageSettings(imageSettings.getRequest());
        }

        HostGroups hostGroups = HostGroups.getTestContextHostGroups().apply(integrationTestContext);
        if (hostGroups != null) {
            stackEntity.getRequest().setInstanceGroups(hostGroups.getRequest());
        }

        logJSON("Stack post request: ", stackEntity.getRequest());
        stackEntity.setResponse(
                client.getCloudbreakClient()
                        .stackV2Endpoint()
                        .postPrivate(stackEntity.getRequest()));
        logJSON("Stack post response: ", stackEntity.getResponse());
    }
}
