package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakIdentityClient;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakIdentityClient cloudbreakClient;

    public StackV4Response getById(long cloudbreakStackId) {
        return cloudbreakClient.autoscaleEndpoint().get(cloudbreakStackId);
    }

    public void failureReport(long stackId, FailureReportV4Request failureReport) {
        cloudbreakClient.autoscaleEndpoint().failureReport(stackId, failureReport);
    }
}
