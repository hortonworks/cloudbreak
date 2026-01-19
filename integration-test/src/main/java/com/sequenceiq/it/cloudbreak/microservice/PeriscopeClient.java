package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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

    private AutoscaleUserCrnClient alternativePeriscopeClient;

    public PeriscopeClient(CloudbreakUser cloudbreakUser, String periscopeAddress, String alternativePeriscopeAddress) {
        setActing(cloudbreakUser);
        periscopeClient = createPeriscopeClient(periscopeAddress);

        if (isNotEmpty(alternativePeriscopeAddress)) {
            alternativePeriscopeClient = createPeriscopeClient(alternativePeriscopeAddress);
        }
    }

    private AutoscaleUserCrnClient createPeriscopeClient(String periscopeAddress) {
        return new AutoscaleUserCrnClientBuilder(periscopeAddress)
                .withDebug(true)
                .withCertificateValidation(false)
                .withIgnorePreValidation(false)
                .build();
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        LOGGER.info("Flow is not supported by periscope client");
        return null;
    }

    @Override
    public WaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, Status> desiredStatuses,
            TestContext testContext, Set<Status> ignoredFailedStatuses) {
        return null;
    }

    @Override
    public com.sequenceiq.periscope.client.AutoscaleUserCrnClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativePeriscopeClient;
        } else {
            return periscopeClient;
        }
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(
                AutoScaleConfigDto.class.getSimpleName());
    }
}

