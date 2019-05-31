package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    public StackV4Response getById(long cloudbreakStackId) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().get(cloudbreakStackId);
    }

    public void failureReport(long stackId, FailureReportV4Request failureReport) {
        cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().failureReport(stackId, failureReport);
    }
}
