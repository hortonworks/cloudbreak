package com.sequenceiq.cloudbreak.cloud.openstack.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;

import org.openstack4j.api.exceptions.ServerResponseException;
import org.openstack4j.api.exceptions.StatusCode;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Service;
import org.openstack4j.model.identity.v3.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequestBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;

@Component
public class OpenStackClusterProxyService {

    public static final String KEYSTONE_SERVICE_NAME = "keystone";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackClusterProxyService.class);

    @Value("${cb.openstack.disable.ssl.verification:false}")
    private boolean disableSSLVerification;

    @Inject
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public void registerServices(KeystoneCredentialView credential) {
        String credentialCrn = credential.getCredentialCrn();
        String accountId = Crn.safeFromString(credentialCrn).getAccountId();
        String jumpgateEnvironmentCrn = credential.getJumpgateEnvironmentCrn();

        try {
            LOGGER.info("Registering OpenStack keystone with cluster proxy. credentialCrn={}, jumpgateEnvironmentCrn={}", credentialCrn, jumpgateEnvironmentCrn);
            registerKeystoneService(credentialCrn, jumpgateEnvironmentCrn, accountId, credential.getEndpoint());

            String keystoneProxyUrl = buildProxyUrl(credentialCrn, KEYSTONE_SERVICE_NAME);
            LOGGER.info("Authenticating to keystone through cluster proxy at {}", keystoneProxyUrl);

            Token token = authenticateThroughProxy(keystoneProxyUrl, credential);
            List<ClusterServiceConfig> serviceConfigs = buildServiceConfigsFromCatalog(token, credential);
            if (!serviceConfigs.isEmpty()) {
                LOGGER.info("Registering {} services from catalog with cluster proxy", serviceConfigs.size());
                registerAllServices(credentialCrn, jumpgateEnvironmentCrn, accountId, serviceConfigs);
            }
        } catch (ServerResponseException e) {
            LOGGER.error("Failed to register services through cluster proxy", e);
            safeDeregister(credentialCrn);
            if (StatusCode.BAD_GATEWAY.equals(e.getStatusCode())) {
                throw new UnauthorizedException("Failed to authenticate through cluster proxy, jumpgate agent is not reachable");
            } else {
                throw new UnauthorizedException("Failed to authenticate through cluster proxy: " + e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register services through cluster proxy", e);
            safeDeregister(credentialCrn);
            throw new UnauthorizedException("Failed to register services through cluster proxy: " + e.getMessage());
        }
    }

    public boolean isRegistered(String credentialCrn) {
        try {
            ReadConfigResponse response = clusterProxyRegistrationClient.readConfig(credentialCrn);
            return response != null && response.getServices() != null && !response.getServices().isEmpty();
        } catch (ClusterProxyException e) {
            LOGGER.debug("Cluster proxy config not found for credentialCrn={}, registration needed", credentialCrn);
            return false;
        }
    }

    public void deregisterServices(String credentialCrn) {
        LOGGER.info("Deregistering OpenStack services from cluster proxy. credentialCrn={}", credentialCrn);
        clusterProxyRegistrationClient.deregisterConfig(credentialCrn);
    }

    private void safeDeregister(String credentialCrn) {
        try {
            deregisterServices(credentialCrn);
        } catch (Exception deregisterEx) {
            LOGGER.warn("Failed to deregister services during cleanup for credentialCrn='{}', ignoring", credentialCrn, deregisterEx);
        }
    }

    public String buildProxyBaseUrl(String credentialCrn) {
        return clusterProxyConfiguration.getClusterProxyUrl()
                + clusterProxyConfiguration.getHttpProxyPath() + "/" + credentialCrn;
    }

    public String buildProxyUrl(String credentialCrn, String serviceName) {
        return buildProxyBaseUrl(credentialCrn) + "/" + serviceName;
    }

    private void registerKeystoneService(String credentialCrn, String environmentCrn, String accountId, String keystoneEndpoint) {
        ClusterServiceConfig keystoneConfig = new ClusterServiceConfig(
                KEYSTONE_SERVICE_NAME, List.of(keystoneEndpoint), null, false, null, null, null);

        ConfigRegistrationRequest request = buildRegistrationRequest(credentialCrn, environmentCrn, accountId, List.of(keystoneConfig));
        clusterProxyRegistrationClient.registerConfig(request);
        LOGGER.info("Keystone service registered with cluster proxy");
    }

    private Token authenticateThroughProxy(String keystoneProxyUrl, KeystoneCredentialView credential) {
        Config config = Config.newConfig();
        if (disableSSLVerification) {
            config.withSSLVerificationDisabled();
        }
        return KeystoneTokenFactory.authenticate(config, keystoneProxyUrl, credential);
    }

    private List<ClusterServiceConfig> buildServiceConfigsFromCatalog(Token token, KeystoneCredentialView credential) {
        Set<String> registeredNames = new TreeSet<>(Comparator.naturalOrder());
        List<ClusterServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(new ClusterServiceConfig(
                KEYSTONE_SERVICE_NAME, List.of(credential.getEndpoint()), null, false, null, null, null));
        registeredNames.add(KEYSTONE_SERVICE_NAME);

        List<? extends Service> catalog = token.getCatalog();
        if (catalog == null) {
            throw new IllegalStateException("Token has no service catalog, cannot discover OpenStack services");
        }
        for (Service service : catalog) {
            ServiceType serviceType = ServiceType.forName(service.getName());
            if (serviceType == ServiceType.UNKNOWN) {
                LOGGER.debug("Skipping unknown service type: catalogName={}", service.getName());
                continue;
            }
            String resolvedName = serviceType.getServiceName();
            if (!registeredNames.add(resolvedName)) {
                LOGGER.debug("Skipping duplicate service: catalogName={}, resolvedAs={}", service.getName(), resolvedName);
                continue;
            }
            List<String> endpoints = collectEndpointsByFacing(service, credential);
            if (!endpoints.isEmpty()) {
                LOGGER.debug("Adding service from catalog: catalogName={}, registeredAs={}, endpoints={}",
                        service.getName(), resolvedName, endpoints);
                serviceConfigs.add(new ClusterServiceConfig(
                        resolvedName, endpoints, null, false, null, null, null));
            }
        }
        return serviceConfigs;
    }

    private List<String> collectEndpointsByFacing(Service service, KeystoneCredentialView credential) {
        List<String> endpoints = new ArrayList<>();
        for (Endpoint endpoint : service.getEndpoints()) {
            if (matchesFacing(endpoint, credential)) {
                String url = endpoint.getUrl().toString();
                if (!endpoints.contains(url)) {
                    endpoints.add(url);
                }
            }
        }
        return endpoints;
    }

    private boolean matchesFacing(Endpoint endpoint, KeystoneCredentialView credential) {
        if (endpoint.getIface() == null) {
            return false;
        }
        String facing = credential.getFacing();
        return endpoint.getIface().name().equalsIgnoreCase(facing);
    }

    private void registerAllServices(String credentialCrn, String environmentCrn, String accountId,
            List<ClusterServiceConfig> serviceConfigs) {
        ConfigRegistrationRequest request = buildRegistrationRequest(credentialCrn, environmentCrn, accountId, serviceConfigs);
        clusterProxyRegistrationClient.registerConfig(request);
        LOGGER.info("All OpenStack services registered with cluster proxy: {}",
                serviceConfigs.stream().map(ClusterServiceConfig::toString).toList());
    }

    private ConfigRegistrationRequest buildRegistrationRequest(String credentialCrn, String environmentCrn,
            String accountId, List<ClusterServiceConfig> services) {
        return new ConfigRegistrationRequestBuilder(credentialCrn)
                .withEnvironmentCrn(environmentCrn)
                .withAccountId(accountId)
                .withServices(services)
                .withUseCcmV2(true)
                .withTlsStrictCheck(false)
                .build();
    }
}
