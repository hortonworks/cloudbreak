package com.sequenceiq.cloudbreak.service.proxy;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;

@Service
public class ProxyConfigDtoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigDtoService.class);

    @Inject
    private ProxyEndpoint proxyEndpoint;

    @Inject
    private SecretService secretService;

    public ProxyConfig getByName(String resourceName) {
        return convert(getProxyConfig(resourceName, proxyEndpoint::getByName));
    }

    public ProxyConfig getByCrn(String resourceCrn) {
        return convert(getProxyConfig(resourceCrn, proxyEndpoint::getByResourceCrn));
    }

    public Optional<ProxyConfig> getByCrnWithEnvironmentFallback(String resourceCrn, @Nonnull String environmentCrn) {
        if (!StringUtils.isEmpty(resourceCrn)) {
            return Optional.ofNullable(getByCrn(resourceCrn));
        } else {
            return getByEnvironmentCrn(environmentCrn);
        }
    }

    public Optional<ProxyConfig> getByEnvironmentCrn(String environmentCrn) {
        try {
            return Optional.ofNullable(convert(proxyEndpoint.getByEnvironmentCrn(environmentCrn)));
        } catch (NotFoundException ex) {
            return Optional.empty();
        }
    }

    public boolean isProxyConfiguredForEnvironment(String environmentCrn) {
        try {
            return proxyEndpoint.getByEnvironmentCrn(environmentCrn) != null;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    private ProxyConfig convert(ProxyResponse proxyResponse) {
        ProxyConfig.ProxyConfigBuilder proxyConfigBuilder = ProxyConfig.builder()
                .withName(proxyResponse.getName())
                .withCrn(proxyResponse.getCrn())
                .withProtocol(proxyResponse.getProtocol())
                .withServerHost(proxyResponse.getHost())
                .withServerPort(proxyResponse.getPort());
        if (proxyResponse.getUserName() != null && proxyResponse.getPassword() != null) {
            proxyConfigBuilder.withProxyAuthentication(ProxyAuthentication.builder()
                    .withUserName(getSecret(proxyResponse.getUserName()))
                    .withPassword(getSecret(proxyResponse.getPassword())).build());
        }
        return proxyConfigBuilder.build();
    }

    private ProxyResponse getProxyConfig(String value, Function<String, ProxyResponse> function) {
        try {
            return function.apply(value);
        } catch (WebApplicationException | ProcessingException e) {
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
