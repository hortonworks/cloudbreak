package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    public StackResponse getById(long cloudbreakStackId) {
        CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
        return cloudbreakClient.autoscaleEndpoint().get(cloudbreakStackId);
    }

    public void failureReport(long stackId, FailureReport failureReport) {
        CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
        cloudbreakClient.recoveryEndpoint().failureReport(stackId, failureReport);
    }
}
