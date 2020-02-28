package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    public StackV4Response getByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().get(stackCrn);
    }

}
