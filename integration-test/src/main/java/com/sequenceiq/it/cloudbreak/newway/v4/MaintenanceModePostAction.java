package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class MaintenanceModePostAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceModePostAction.class);

    private MaintenanceModeV4Request request = new MaintenanceModeV4Request();

    public MaintenanceModePostAction withStatus(MaintenanceModeStatus status) {
        request.setStatus(status);
        return this;
    }

    public static MaintenanceModePostAction enable() {
        return new MaintenanceModePostAction().withStatus(MaintenanceModeStatus.ENABLED);
    }

    public static MaintenanceModePostAction disable() {
        return new MaintenanceModePostAction().withStatus(MaintenanceModeStatus.DISABLED);
    }

    public static MaintenanceModePostAction validate() {
        return new MaintenanceModePostAction().withStatus(MaintenanceModeStatus.VALIDATION_REQUESTED);
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {

        logJSON(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .setClusterMaintenanceMode(client.getWorkspaceId(), entity.getName(), request);

        return entity;
    }
}
