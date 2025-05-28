package com.sequenceiq.freeipa.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public void setFreeIpaNodeCount(String envCrn, int nodeCount) {
        try {
            EnvironmentEditRequest environmentEditRequest = new EnvironmentEditRequest();
            environmentEditRequest.setFreeIpaNodeCount(nodeCount);
            LOGGER.debug("Modifying freeIpa count to {} on {} environment.", nodeCount, envCrn);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> environmentEndpoint.editByCrn(envCrn, environmentEditRequest)
            );
        } catch (ClientErrorException e) {
            try (Response response = e.getResponse()) {
                if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                    throw new BadRequestException(String.format("Environment not found by environment CRN: %s", envCrn), e);
                }
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                throw new CloudbreakServiceException(String.format("Failed to get environment: %s", errorMessage), e);
            }
        }
    }

    public boolean isSecretEncryptionEnabled(String environmentCrn) {
        return environmentEndpoint.getByCrn(environmentCrn).isEnableSecretEncryption();
    }
}
