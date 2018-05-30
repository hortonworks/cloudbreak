package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.IntegrationTestContext;

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

        log(" Name:\n" + stackEntity.getRequest().getGeneral().getName());
        logJSON(" Stack post request:\n", stackEntity.getRequest());
        try {
            stackEntity.setResponse(
                    client.getCloudbreakClient()
                            .stackV2Endpoint()
                            .postPrivate(stackEntity.getRequest()));
        } catch (WebApplicationException e) {
            logJSON(" Stack post response:\n", e.getResponse().readEntity(String.class));
            throw e;
        }
        logJSON(" Stack post response:\n", stackEntity.getResponse());
        log(" ID:\n" + stackEntity.getResponse().getId());
    }
}
