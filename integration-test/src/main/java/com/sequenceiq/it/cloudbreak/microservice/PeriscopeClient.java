package com.sequenceiq.it.cloudbreak.microservice;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.autoscale.AutoScaleConfigDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.periscope.client.AutoscaleUserCrnClient;
import com.sequenceiq.periscope.client.AutoscaleUserCrnClientBuilder;
import com.sequenceiq.redbeams.api.model.common.Status;

public class PeriscopeClient extends MicroserviceClient<com.sequenceiq.periscope.client.AutoscaleUserCrnClient, Void, Status, WaitObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriscopeClient.class);

    private AutoscaleUserCrnClient periscopeClient;

    public PeriscopeClient(CloudbreakUser cloudbreakUser, String periscopeAddress) {
        setActing(cloudbreakUser);
        periscopeClient = new AutoscaleUserCrnClientBuilder(periscopeAddress)
                .withDebug(true)
                .withCertificateValidation(false)
                .withIgnorePreValidation(false)
                .build();
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        LOGGER.info("Flow is not supported by periscope client");
        return null;
    }

    @Override
    public WaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, Status> desiredStatuses,
            TestContext testContext, Set<Status> ignoredFailedStatuses) {
        return null;
    }

    @Override
    public com.sequenceiq.periscope.client.AutoscaleUserCrnClient getDefaultClient() {
        return periscopeClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                AutoScaleConfigDto.class.getSimpleName());
    }
}

