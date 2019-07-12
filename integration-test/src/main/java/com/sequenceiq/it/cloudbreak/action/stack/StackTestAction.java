package com.sequenceiq.it.cloudbreak.action.stack;

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackTestAction {

    public static final String INSTANCE_ID = "SDA-instanceId";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTestAction.class);

    private StackTestAction() {

    }

    public static StackTestDto delete(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack delete request:\n", entity.getRequest());
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .delete(client.getWorkspaceId(), entity.getName(), false, false);
        Log.logJSON(LOGGER, " Stack deletion was successful:\n", entity.getResponse());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto deleteInstance(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.logJSON(LOGGER, " Stack delete instance request:\n", entity.getRequest());
        String instanceId = testContext.getRequiredSelected(INSTANCE_ID);
        Boolean forced = testContext.getSelected("forced");
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .deleteInstance(client.getWorkspaceId(), entity.getName(), forced != null && forced, instanceId);
        Log.logJSON(LOGGER, " Stack delete instance was successful:\n", entity.getResponse());
        return entity;
    }

    public static StackTestDto create(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, " Name: " + entity.getRequest().getName());
        Log.logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        Log.logJSON(LOGGER, " Stack created was successfully:\n", entity.getResponse());
        Log.log(LOGGER, " CRN: " + entity.getResponse().getCrn());

        return entity;
    }

    public static StackTestDto get(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, " Name: " + entity.getRequest().getName());
        Log.logJSON(LOGGER, " Stack get request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .get(client.getWorkspaceId(), entity.getName(), new HashSet<>()));
        Log.logJSON(LOGGER, " Stack get was successfully:\n", entity.getResponse());
        Log.log(LOGGER, " CRN: " + entity.getResponse().getCrn());

        return entity;
    }

    public static StackTestDto refresh(TestContext testContext, StackTestDto entity, CloudbreakClient client) {
        entity.setResponse(
                client.getCloudbreakClient().stackV4Endpoint().get(client.getWorkspaceId(), entity.getName(), Collections.emptySet())
        );
        return entity;
    }

    public static StackTestDto start(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        client.getCloudbreakClient().stackV4Endpoint().putStart(client.getWorkspaceId(), entity.getName());
        Log.logJSON(LOGGER, " Stack was started successfully:\n", entity.getResponse());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto stop(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        client.getCloudbreakClient().stackV4Endpoint().putStop(client.getWorkspaceId(), entity.getName());
        Log.logJSON(LOGGER, " Stack was stopped successfully:\n", entity.getResponse());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto modifyAmbariPassword(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack put ambari password request:\n", entity.getRequest());
        UserNamePasswordV4Request userNamePasswordV4Request = new UserNamePasswordV4Request();
        userNamePasswordV4Request.setOldPassword(entity.getRequest().getCluster().getPassword());
        userNamePasswordV4Request.setUserName(entity.getRequest().getCluster().getUserName());
        userNamePasswordV4Request.setPassword("testnewambaripassword");
        client.getCloudbreakClient().stackV4Endpoint().putPassword(client.getWorkspaceId(),
                entity.getName(), userNamePasswordV4Request);
        Log.logJSON(LOGGER, " Stack was modified ambari password successfully:\n", entity.getResponse());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto sync(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack post request:\n", entity.getRequest());
        client.getCloudbreakClient().stackV4Endpoint().putSync(client.getWorkspaceId(), entity.getName());
        Log.logJSON(LOGGER, " Stack sync was successful:\n", entity.getResponse());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }

    public static StackTestDto getBlueprintByRequest(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack get blueprint:\n", entity.getRequest());
        GeneratedBlueprintV4Response bp = client.getCloudbreakClient().stackV4Endpoint().postStackForBlueprint(
                client.getWorkspaceId(),
                entity.getName(),
                entity.getRequest());
        entity.withGeneratedBlueprint(bp);
        Log.logJSON(LOGGER, " get blueprint was successfully:\n", entity.getGeneratedBlueprint());
        return entity;
    }

    public static StackTestDto getCli(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        Log.logJSON(LOGGER, " Stack get cli skeleton:\n", entity.getRequest());
        StackV4Request request = client.getCloudbreakClient().stackV4Endpoint().getRequestfromName(
                client.getWorkspaceId(),
                entity.getName());
        entity.setRequest(request);
        Log.logJSON(LOGGER, " get cli skeleton was successfully:\n", entity.getRequest());
        Log.log(LOGGER, format(" CRN: %s", entity.getResponse().getCrn()));
        return entity;
    }
}
