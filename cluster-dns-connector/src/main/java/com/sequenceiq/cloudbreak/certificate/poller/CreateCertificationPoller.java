package com.sequenceiq.cloudbreak.certificate.poller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse.SigningStatus;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;

public class CreateCertificationPoller implements AttemptMaker<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCertificationPoller.class);

    private GrpcClusterDnsClient grpcClusterDnsClient;

    private String workflowId;

    private Optional<String> requestId;

    private int attempt;

    public CreateCertificationPoller(GrpcClusterDnsClient grpcClusterDnsClient, String workflowId, Optional<String> requestId) {
        this.grpcClusterDnsClient = grpcClusterDnsClient;
        this.workflowId = workflowId;
        this.requestId = requestId;
    }

    @Override
    public AttemptResult<List<String>> process() throws Exception {
        attempt++;
        PollCertificateSigningResponse response = grpcClusterDnsClient.pollCertificateSigning(workflowId, requestId);
        final SigningStatus actualStatus = response.getStatus();
        LOGGER.debug("Polling attempt result: {}", actualStatus);
        if (SigningStatus.SUCCEEDED.equals(actualStatus)) {
            return AttemptResults.finishWith(response.getCertificatesList());
        }
        if (SigningStatus.FAILED.equals(actualStatus)) {
            //TODO integrate the message for detailed cause when it will be implemented on PEM side.
            //return AttemptResults.breakFor(String.format("Certificate creation is failed for %s with message: %s ", workflowId));
            return AttemptResults.breakFor(String.format("Certificate creation is failed for %s.", workflowId));
        }
        LOGGER.info("The certificate polls continued, attempt: {}", attempt);
        return AttemptResults.justContinue();
    }
}
