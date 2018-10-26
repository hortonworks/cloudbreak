package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakClient cloudbreakClient;

    public StackResponse getById(long cloudbreakStackId) {
        return cloudbreakClient.autoscaleEndpoint().get(cloudbreakStackId);
    }

    public void failureReport(long stackId, FailureReport failureReport) {
        cloudbreakClient.autoscaleEndpoint().failureReport(stackId, failureReport);
    }
}
