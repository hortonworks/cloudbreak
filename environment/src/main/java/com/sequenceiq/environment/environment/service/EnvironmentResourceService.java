package com.sequenceiq.environment.environment.service;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class EnvironmentResourceService {

    private final CredentialService credentialService;

    private final NetworkService networkService;

    public EnvironmentResourceService(CredentialService credentialService, NetworkService networkService) {
        this.credentialService = credentialService;
        this.networkService = networkService;
    }

    public Credential getCredentialFromRequest(CredentialAwareEnvRequest request, String accountId, String creator) {
        Credential credential;
        if (StringUtils.isNotEmpty(request.getCredentialName())) {
            try {
                credential = credentialService.getByNameForAccountId(request.getCredentialName(), accountId);
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No credential found with name [%s] in the workspace.",
                        request.getCredentialName()), e);
            }
        } else {
            throw new BadRequestException("No credential has been specified in request for environment creation.");
        }
        return credential;
    }

    BaseNetwork createAndSetNetwork(Environment environment, NetworkDto networkDto, String accountId) {
        BaseNetwork network = networkService.saveNetwork(environment, networkDto, accountId);
        if (network != null) {
            environment.setNetwork(network);
        }
        return network;
    }

    private void validatePlatform(Environment environment, String requestedPlatform) {
        if (!environment.getCloudPlatform().equals(requestedPlatform)) {
            throw new BadRequestException(String.format("The requested credential's cloud platform [%s] "
                    + "does not match with the environments cloud platform [%s].", requestedPlatform, environment.getCloudPlatform()));
        }
    }
}
