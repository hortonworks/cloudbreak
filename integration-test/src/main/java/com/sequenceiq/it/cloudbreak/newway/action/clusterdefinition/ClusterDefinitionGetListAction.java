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

public class ClusterDefinitionGetListAction implements Action<ClusterDefinitionEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionGetListAction.class);

    @Override
    public ClusterDefinitionEntity action(TestContext testContext, ClusterDefinitionEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Cluster definition list by workspace request:%n"), entity.getRequest());
        entity.setViewResponses(
                client.getCloudbreakClient()
                        .clusterDefinitionV4Endpoint()
                        .list(client.getWorkspaceId()).getResponses());
        logJSON(LOGGER, format(" Cluster definition list has executed successfully:%n"), entity.getViewResponses());

        return entity;
    }

}