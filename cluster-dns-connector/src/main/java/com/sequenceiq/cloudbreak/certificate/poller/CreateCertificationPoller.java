package com.sequenceiq.cloudbreak.certificate.poller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateCreationResponse;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;

public class CreateCertificationPoller implements AttemptMaker<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCertificationPoller.class);

    private GrpcClusterDnsClient grpcClusterDnsClient;

    private String actorCrn;

    private String pollerRequestId;

    private Optional<String> requestId;

    public CreateCertificationPoller(GrpcClusterDnsClient grpcClusterDnsClient, String actorCrn, String pollerRequestId, Optional<String> requestId) {
        this.grpcClusterDnsClient = grpcClusterDnsClient;
        this.actorCrn = actorCrn;
        this.pollerRequestId = pollerRequestId;
        this.requestId = requestId;
    }

    @Override
    public AttemptResult<List<String>> process() throws Exception {
        PollCertificateCreationResponse response = grpcClusterDnsClient.pollCreateCertificate(actorCrn, pollerRequestId, requestId);
        LOGGER.debug("Polling attempt result: {}", response.getStatus());
        if (response.getStatus().equalsIgnoreCase("succeeded")) {
            return AttemptResults.finishWith(response.getCertificatesList());
        }
        if (response.getStatus().equalsIgnoreCase("FAILED")) {
            return AttemptResults.breakFor(String.format("Certificate creation is failed for %s with status: %s ", pollerRequestId, response.getStatus()));
        }
        LOGGER.debug("Polling continues");
        return AttemptResults.justContinue();
    }
}
