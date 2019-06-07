package com.sequenceiq.cloudbreak.service.proxy;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.client.EnvironmentServiceClient;

@Service
public class ProxyConfigDtoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigDtoService.class);

    @Inject
    private EnvironmentServiceClient environmentServiceClient;

    @Inject
    private SecretService secretService;

    public ProxyConfig get(String resourceCrn, String accountId, String userCrn) {
        ProxyEndpoint proxyEndpoint = environmentServiceClient
                .withCrn(userCrn)
                .proxyV1Endpoint();

        ProxyResponse proxyResponse = getProxyConfig(resourceCrn, proxyEndpoint);

        return ProxyConfig.builder()
                .withName(proxyResponse.getName())
                .withCrn(proxyResponse.getCrn())
                .withUserCrn(userCrn)
                .withAccountId(accountId)
                .withProtocol(proxyResponse.getProtocol())
                .withServerHost(proxyResponse.getHost())
                .withServerPort(proxyResponse.getPort())
                .withUserName(getSecret(proxyResponse.getUserName()))
                .withPassword(getSecret(proxyResponse.getPassword()))
                .build();
    }

    private ProxyResponse getProxyConfig(String resourceCrn, ProxyEndpoint proxyEndpoint) {
        try {
            return proxyEndpoint.getByResourceCrn(resourceCrn);
        } catch (WebApplicationException e) {
            String message = String.format("Failed to get Proxy config from Environment service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private String getSecret(SecretResponse userName) {
        try {
            return secretService.getByResponse(userName);
        } catch (VaultException e) {
            String message = String.format("Failed to get Proxy config related secret due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
