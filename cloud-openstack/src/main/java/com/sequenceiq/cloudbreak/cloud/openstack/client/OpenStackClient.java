package com.sequenceiq.cloudbreak.cloud.openstack.client;

import static com.google.common.collect.Lists.newArrayList;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.FACING;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ServerResponseException;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.ext.AvailabilityZone;
import org.openstack4j.model.heat.Resource;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Service;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class OpenStackClient {

    public static final String OS_NEUTRON_SUBNET = "OS::Neutron::Subnet";

    public static final String OS_NEUTRON_NET = "OS::Neutron::Net";

    public static final String OS_NEUTRON_FLOATING_IP = "OS::Neutron::FloatingIP";

    public static final String OS_TROVE_INSTANCE = "OS::Trove::Instance";

    public static final String OS_CINDER_VOLUME_ATTACHMENT = "OS::Cinder::VolumeAttachment";

    public static final String OS_NEUTRON_PORT = "OS::Neutron::Port";

    public static final String OS_NEUTRON_SECURITY_GROUP = "OS::Neutron::SecurityGroup";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackClient.class);

    @Value("${cb.openstack.api.debug:true}")
    private boolean debug;

    @Value("${cb.openstack.disable.ssl.verification:false}")
    private boolean disableSSLVerification;

    @Inject
    private OpenStackClusterProxyService clusterProxyService;

    private final Config config = Config.newConfig();

    @PostConstruct
    public void init() {
        OSFactory.enableHttpLoggingFilter(debug);
        if (disableSSLVerification) {
            config.withSSLVerificationDisabled();
        }
    }

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        createAccessOrToken(authenticatedContext);
        return authenticatedContext;
    }

    public OSClient<?> createOSClient(AuthenticatedContext authenticatedContext) {
        String facing = authenticatedContext.getCloudCredential().getStringParameter(FACING);
        Token token = authenticatedContext.getParameter(Token.class);
        KeystoneCredentialView credential = createKeystoneCredential(authenticatedContext);
        if (StringUtils.isNotBlank(credential.getJumpgateEnvironmentCrn())) {
            Config jumpgateConfig = Config.newConfig()
                    .withEndpointURLResolver(createJumpgateResolver(authenticatedContext.getCloudCredential().getId()));
            return OSFactory.clientFromToken(token, Facing.value(facing), jumpgateConfig);
        }
        return OSFactory.clientFromToken(token, Facing.value(facing));
    }

    public OSClient<?> createOSClient(CloudCredential cloudCredential) {
        String facing = cloudCredential.getStringParameter(FACING);
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);
        Token token = createToken(osCredential, cloudCredential.getId());
        if (StringUtils.isNotBlank(osCredential.getJumpgateEnvironmentCrn())) {
            Config jumpgateConfig = Config.newConfig()
                    .withEndpointURLResolver(createJumpgateResolver(cloudCredential.getId()));
            return OSFactory.clientFromToken(token, Facing.value(facing), jumpgateConfig);
        }
        return OSFactory.clientFromToken(token, Facing.value(facing));
    }

    private OpenStackJumpgateEndpointURLResolver createJumpgateResolver(String credentialCrn) {
        return new OpenStackJumpgateEndpointURLResolver(clusterProxyService.buildProxyBaseUrl(credentialCrn));
    }

    public KeystoneCredentialView createKeystoneCredential(CloudCredential cloudCredential) {
        return new KeystoneCredentialView(cloudCredential);
    }

    public KeystoneCredentialView createKeystoneCredential(AuthenticatedContext authenticatedContext) {
        return new KeystoneCredentialView(authenticatedContext);
    }

    public List<CloudResource> getResources(String stackName, CloudCredential cloudCredential) {
        return newArrayList(createOSClient(cloudCredential).heat().resources().list(stackName)
                .stream()
                .map(r -> {
                    Optional<ResourceType> type = getType(r);
                    return type.map(resourceType -> CloudResource.builder()
                            .withName(r.getPhysicalResourceId())
                            .withType(resourceType)
                            .build()).orElse(null);
                })
                .filter(r -> Objects.nonNull(r) && StringUtils.isNotBlank(r.getName()))
                .collect(Collectors.toMap(CloudResource::getName, cloudResource -> cloudResource))
                .values());
    }

    private Optional<ResourceType> getType(Resource resource) {
        ResourceType result = null;
        switch (resource.getType()) {
            case OS_NEUTRON_SUBNET:
                result = ResourceType.OPENSTACK_SUBNET;
                break;
            case OS_NEUTRON_NET:
                result = ResourceType.OPENSTACK_NETWORK;
                break;
            case OS_TROVE_INSTANCE:
                result = ResourceType.OPENSTACK_INSTANCE;
                break;
            case OS_CINDER_VOLUME_ATTACHMENT:
                result = ResourceType.OPENSTACK_ATTACHED_DISK;
                break;
            case OS_NEUTRON_PORT:
                result = ResourceType.OPENSTACK_PORT;
                break;
            case OS_NEUTRON_SECURITY_GROUP:
                result = ResourceType.OPENSTACK_SECURITY_GROUP;
                break;
            case OS_NEUTRON_FLOATING_IP:
                result = ResourceType.OPENSTACK_FLOATING_IP;
                break;
            default:
                LOGGER.debug("Not a valid resource type for OS: {}", resource.getType());
                break;
        }

        return Optional.ofNullable(result);
    }

    private Token createToken(KeystoneCredentialView osCredential, String credentialCrn) {
        if (osCredential == null) {
            throw new CredentialVerificationException("Empty credential");
        }
        if (osCredential.getScope() == null) {
            throw new CredentialVerificationException("Null scope not supported");
        }
        String endpoint = resolveKeystoneEndpoint(osCredential, credentialCrn);
        try {
            return KeystoneTokenFactory.authenticate(config, endpoint, osCredential);
        } catch (AuthenticationException authenticationException) {
            LOGGER.error("Openstack authentication failed, can not create token", authenticationException);
            throw new CredentialVerificationException("Openstack authentication failed, can not create token: " + authenticationException.getMessage(),
                    authenticationException);
        } catch (ServerResponseException serverResponseException) {
            LOGGER.error("Openstack authentication failed, ServerResponseException was thrown", serverResponseException);
            throw new CredentialVerificationException("Openstack authentication failed, can not create token: " + serverResponseException.getMessage(),
                    serverResponseException);
        }
    }

    private String resolveKeystoneEndpoint(KeystoneCredentialView osCredential, String credentialCrn) {
        if (StringUtils.isNotBlank(osCredential.getJumpgateEnvironmentCrn())) {
            return clusterProxyService.buildProxyUrl(credentialCrn, OpenStackClusterProxyService.KEYSTONE_SERVICE_NAME);
        }
        return osCredential.getEndpoint();
    }

    private void createAccessOrToken(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext.getCloudCredential());
        Token token = createToken(osCredential, authenticatedContext.getCloudCredential().getId());
        if (token != null) {
            authenticatedContext.putParameter(Token.class, token);
        } else {
            throw new CredentialVerificationException("Openstack authentication failed, can not create token");
        }
    }

    public Set<String> getRegion(CloudCredential cloudCredential) {
        KeystoneCredentialView keystoneCredential = createKeystoneCredential(cloudCredential);
        Set<String> regions = new HashSet<>();
        Token token = createToken(keystoneCredential, cloudCredential.getId());
        for (Service service : token.getCatalog()) {
            for (Endpoint endpoint : service.getEndpoints()) {
                regions.add(endpoint.getRegion());
            }
        }
        LOGGER.debug("Regions from openstack: {}", regions);
        return regions;
    }

    public List<com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone> getZones(OSClient<?> osClient, String regionFromOpenStack) {
        List<? extends AvailabilityZone> zonesFromOS = osClient.compute().zones().list();
        LOGGER.debug("Zones from openstack for {}: {}", regionFromOpenStack, zonesFromOS);
        return zonesFromOS.stream().map(z -> availabilityZone(z.getZoneName())).collect(Collectors.toList());
    }

    public List<? extends Flavor> getFlavors(OSClient<?> osClient) {
        return osClient.compute().flavors().list();
    }

}