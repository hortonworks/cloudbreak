package com.sequenceiq.freeipa.service;

import java.util.List;

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
import com.sequenceiq.cloudbreak.tls.TlsSpecificationsHelper;
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

    public String getTlsVersions(String environmentCrn, String separator) {
        return TlsSpecificationsHelper.getTlsVersions(separator);
        /* TODO: use API call to get values */
    }

    public String getTlsCipherSuites(String environmentCrn) {
        return getTlsCipherSuites(environmentCrn, TlsSpecificationsHelper.CipherSuitesLimitType.DEFAULT);
    }

    private String[] getTlsCipherSuitesList(String environmentCrn, TlsSpecificationsHelper.CipherSuitesLimitType cipherSuiteLimitType) {
        return TlsSpecificationsHelper.getDefaultCipherSuiteList(cipherSuiteLimitType);
        /* TODO: use API call to get values */
    }

    public String getTlsCipherSuites(String environmentCrn, TlsSpecificationsHelper.CipherSuitesLimitType cipherSuiteLimitType) {
        String[] tlsCipherSuites = getTlsCipherSuitesList(environmentCrn, cipherSuiteLimitType);
        return TlsSpecificationsHelper.getTlsCipherSuites(tlsCipherSuites, cipherSuiteLimitType, ":");

    }

    public List<String> getTlsCipherSuitesIanaList(String environmentCrn, TlsSpecificationsHelper.CipherSuitesLimitType cipherSuitelimitType) {
        String[] tlsCipherSuites = getTlsCipherSuitesList(environmentCrn, cipherSuitelimitType);
        return TlsSpecificationsHelper.getTlsCipherSuitesIanaList(tlsCipherSuites, cipherSuitelimitType);
    }
}
