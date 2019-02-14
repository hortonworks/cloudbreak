package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ClusterDefinitionGetListAction implements ActionV2<ClusterDefinitionEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionGetListAction.class);

    @Override
    public ClusterDefinitionEntity action(TestContext testContext, ClusterDefinitionEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Cluster definition list by workspace request:%n"), entity.getRequest());
        var blueprints = client.getCloudbreakClient()
                .blueprintV3Endpoint()
                .listByWorkspace(client.getWorkspaceId());
        logJSON(LOGGER, format(" Cluster definition list has executed successfully:%n"), blueprints);

        return entity;
    }

}