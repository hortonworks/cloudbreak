package com.sequenceiq.it.cloudbreak.action.stack;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTestAction.class);

    private StackTestAction() {

    }

    public static StackTestDto delete(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack delete request:\n", entity.getRequest());
        client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .delete(client.getWorkspaceId(), entity.getName(), false, testContext.getActingUserCrn().getAccountId());
        Log.whenJson(LOGGER, " Stack deletion was successful:\n", entity.getResponse());
        Log.when(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto create(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack post request:\n", entity.getRequest());
        entity.setResponse(
                client.getDefaultClient(testContext)
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest(), testContext.getActingUserCrn().getAccountId()));
        Log.whenJson(LOGGER, " Stack created was successful:\n", entity.getResponse());
        Log.when(LOGGER, " CRN: " + entity.getResponse().getCrn());

        return entity;
    }

    public static StackTestDto get(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack get request:\n", entity.getRequest());
        entity.setResponse(
                client.getDefaultClient(testContext)
                        .stackV4Endpoint()
                        .get(client.getWorkspaceId(), entity.getName(), new HashSet<>(),
                                testContext.getActingUserCrn().getAccountId()));
        Log.whenJson(LOGGER, " Stack get was successful:\n", entity.getResponse());
        Log.when(LOGGER, " CRN: " + entity.getResponse().getCrn());

        return entity;
    }

    public static StackTestDto refresh(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws IOException {
        entity.setResponse(
                client.getDefaultClient(testContext).stackV4Endpoint().get(client.getWorkspaceId(), entity.getName(), Collections.emptySet(),
                        testContext.getActingUserCrn().getAccountId())
        );
        Log.whenJson(LOGGER, " Stack refresh (get) was successful:\n", entity.getResponse());
        return entity;
    }

    public static StackTestDto start(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack post request:\n", entity.getRequest());
        client.getDefaultClient(testContext).stackV4Endpoint().putStart(client.getWorkspaceId(), entity.getName(),
                testContext.getActingUserCrn().getAccountId());
        Log.whenJson(LOGGER, " Stack was started successful:\n", entity.getResponse());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto stop(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " Stack post request:\n", entity.getRequest());
        client.getDefaultClient(testContext).stackV4Endpoint().putStop(client.getWorkspaceId(), entity.getName(),
                testContext.getActingUserCrn().getAccountId());
        Log.whenJson(LOGGER, " Stack was stopped successful:\n", entity.getResponse());
        Log.when(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }
}
