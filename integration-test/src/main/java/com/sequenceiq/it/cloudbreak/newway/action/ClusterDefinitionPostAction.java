package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ClusterDefinitionPostAction implements ActionV2<ClusterDefinitionEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionPostAction.class);

    @Override
    public ClusterDefinitionEntity action(TestContext testContext, ClusterDefinitionEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Cluster definition post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV3Endpoint()
                        .createInWorkspace(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Cluster definition created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}