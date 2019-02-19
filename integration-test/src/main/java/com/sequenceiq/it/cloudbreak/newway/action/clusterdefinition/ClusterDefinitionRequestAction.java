package com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ClusterDefinitionRequestAction implements Action<ClusterDefinitionEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionRequestAction.class);

    @Override
    public ClusterDefinitionEntity action(TestContext testContext, ClusterDefinitionEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getName()));
        logJSON(LOGGER, format(" Cluster definition request request:%n"), entity.getName());
        entity.setRequest(
                client.getCloudbreakClient()
                        .clusterDefinitionV4Endpoint()
                        .getRequest(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Cluster definition requested successfully:%n"), entity.getRequest());

        return entity;
    }

}