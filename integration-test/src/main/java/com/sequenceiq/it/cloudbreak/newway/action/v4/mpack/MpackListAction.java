package com.sequenceiq.it.cloudbreak.newway.action.v4.mpack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;

public class MpackListAction implements Action<MPackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackListAction.class);

    @Override
    public MPackTestDto action(TestContext testContext, MPackTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, "ManagementPack get all");

        Collection<ManagementPackV4Response> responses = cloudbreakClient.getCloudbreakClient()
                .managementPackV4Endpoint()
                .listByWorkspace(cloudbreakClient.getWorkspaceId()).getResponses();

        entity.setResponses(new HashSet<>(responses));
        logJSON(LOGGER, " ManagementPacks got successfully:\n", entity.getResponses());

        return entity;
    }
}
