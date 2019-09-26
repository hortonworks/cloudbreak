package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Service
public class CloudbreakCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakCommunicator.class);

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    public StackResponse getById(long cloudbreakStackId) {
        CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
        return cloudbreakClient.autoscaleEndpoint().get(cloudbreakStackId);
    }

    public void failureReport(long stackId, FailureReport failureReport) {
        CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
        try (Response response = cloudbreakClient.autoscaleEndpoint().failureReport(stackId, failureReport)) {
            if (Status.ACCEPTED.getStatusCode() != response.getStatus()) {
                String message = "Couldn't send failure report to cloudbreak";
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
        }
    }
}
