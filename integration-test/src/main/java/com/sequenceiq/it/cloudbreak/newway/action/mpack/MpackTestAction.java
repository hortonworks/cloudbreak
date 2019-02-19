package com.sequenceiq.it.cloudbreak.newway.action.mpack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;

public class MpackTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpackTestAction.class);

    private MpackTestAction() {

    }

    public static MPackTestDto list(TestContext testContext, MPackTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ManagementPack get all");

        Collection<ManagementPackV4Response> responses = client.getCloudbreakClient()
                .managementPackV4Endpoint()
                .listByWorkspace(client.getWorkspaceId()).getResponses();

        entity.setResponses(new HashSet<>(responses));
        logJSON(LOGGER, " ManagementPacks got successfully:\n", entity.getResponses());

        return entity;
    }

    public static MPackTestDto delete(TestContext testContext, MPackTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ManagementPack name: " + entity.getName());
        log(LOGGER, " ManagementPack delete request");
        entity.setResponse(
                client.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .deleteInWorkspace(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " ManagementPack deleted successfully:\n", entity.getResponse());
        log(LOGGER, "ManagementPack ID: " + entity.getResponse().getId());

        return entity;
    }

    public static MPackTestDto create(TestContext testContext, MPackTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, "ManagementPack name: " + entity.getName());
        logJSON(LOGGER, " ManagementPack post request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .managementPackV4Endpoint()
                        .createInWorkspace(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " ManagementPack created  successfully:\n", entity.getResponse());
        log(LOGGER, "ManagementPack ID: " + entity.getResponse().getId());

        return entity;
    }

}
