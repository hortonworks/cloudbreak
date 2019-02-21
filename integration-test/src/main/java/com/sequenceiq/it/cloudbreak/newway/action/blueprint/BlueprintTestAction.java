package com.sequenceiq.it.cloudbreak.newway.action.blueprint;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintTestDto;

public class BlueprintTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTestAction.class);

    private BlueprintTestAction() {
    }

    public static BlueprintTestDto getByName(TestContext testContext, BlueprintTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().blueprintV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static BlueprintTestDto postV4(TestContext testContext, BlueprintTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Blueprint created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

    public static BlueprintTestDto getV4(TestContext testContext, BlueprintTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getName()));
        logJSON(LOGGER, format(" Blueprint get response:%n"), entity.getName());
        entity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .get(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Blueprint get successfully:%n"), entity.getResponse());

        return entity;
    }

    public static BlueprintTestDto deleteV4(TestContext testContext, BlueprintTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint delete request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Blueprint deleted successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

    public static BlueprintTestDto listV4(TestContext testContext, BlueprintTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Blueprint list by workspace request:%n"), entity.getRequest());
        entity.setViewResponses(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .list(client.getWorkspaceId()).getResponses());
        logJSON(LOGGER, format(" Blueprint list has executed successfully:%n"), entity.getViewResponses());

        return entity;
    }

    public static BlueprintTestDto requestV4(TestContext testContext, BlueprintTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getName()));
        logJSON(LOGGER, format(" Blueprint request request:%n"), entity.getName());
        entity.setRequest(
                client.getCloudbreakClient()
                        .blueprintV4Endpoint()
                        .getRequest(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Blueprint requested successfully:%n"), entity.getRequest());

        return entity;
    }
}