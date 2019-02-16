package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ManagementPackEntity;

public class ManagementPackGetAllAction implements Action<ManagementPackEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackGetAllAction.class);

    @Override
    public ManagementPackEntity action(TestContext testContext, ManagementPackEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ManagementPack get all");

        Collection<ManagementPackV4Response> responses = client.getCloudbreakClient()
                .managementPackV4Endpoint()
                .listByWorkspace(client.getWorkspaceId()).getResponses();

        entity.setResponses(new HashSet<>(responses));
        logJSON(LOGGER, " ManagementPacks got successfully:\n", entity.getResponses());

        return entity;
    }
}
