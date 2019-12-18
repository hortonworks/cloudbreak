package com.sequenceiq.it.cloudbreak.v4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class MaintenanceModePostAction implements Action<StackTestDto, CloudbreakClient> {

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
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {

        Log.whenJson(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .setClusterMaintenanceMode(client.getWorkspaceId(), testDto.getName(), request);

        return testDto;
    }
}
