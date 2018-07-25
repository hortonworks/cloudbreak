package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.google.common.collect.Lists.newArrayList;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.FACING;
import static com.sequenceiq.cloudbreak.type.OpenStackCredentialV3ScopeConstants.CB_KEYSTONE_V3_DEFAULT_SCOPE;
import static com.sequenceiq.cloudbreak.type.OpenStackCredentialV3ScopeConstants.CB_KEYSTONE_V3_DOMAIN_SCOPE;
import static com.sequenceiq.cloudbreak.type.OpenStackCredentialV3ScopeConstants.CB_KEYSTONE_V3_PROJECT_SCOPE;
import static com.sequenceiq.cloudbreak.type.OpenStackCredentialVersionConstants.CB_KEYSTONE_V2;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.ext.AvailabilityZone;
import org.openstack4j.model.heat.Resource;
import org.openstack4j.model.identity.v2.Access;
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
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class OpenStackClient {

    public static final String OS_NEUTRON_SUBNET = "OS::Neutron::Subnet";

    public static final String OS_NEUTRON_NET = "OS::Neutron::Net";

    public static final String OS_NEUTRON_FLOATING_IP = "OS::Neutron::FloatingIP";

    public static final String OS_TROVE_INSTANCE = "OS::Trove::Instance";

    public static final String OS_CINDER_VOLUME_ATTACHMENT = "OS::Cinder::VolumeAttachment";

    public static final String OS_NEUTRON_PORT = "OS::Neutron::Port";

    public static final String OS_NEUTRON_SECURITY_GROUP = "OS::Neutron::SecurityGroup";

    public static final String OS_NEUTRON_ROUTER = "OS::Neutron::Router";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackClient.class);

    @Value("${cb.openstack.api.debug:true}")
    private boolean debug;

    @Value("${cb.openstack.disable.ssl.verification:false}")
    private boolean disableSSLVerification;

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

        if (isV2Keystone(authenticatedContext)) {
            Access access = authenticatedContext.getParameter(Access.class);
            return OSFactory.clientFromAccess(access, Facing.value(facing));
        } else {
            Token token = authenticatedContext.getParameter(Token.class);
            return OSFactory.clientFromToken(token, Facing.value(facing));
        }
    }

    public OSClient<?> createOSClient(CloudCredential cloudCredential) {
        String facing = cloudCredential.getStringParameter(FACING);

        if (isV2Keystone(cloudCredential)) {
            Access access = createAccess(cloudCredential);
            return OSFactory.clientFromAccess(access, Facing.value(facing));
        } else {
            Token token = createToken(cloudCredential);
            return OSFactory.clientFromToken(token, Facing.value(facing));
        }
    }

    public String getV2TenantId(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        String facing = authenticatedContext.getCloudCredential().getStringParameter(FACING);
        Access access = authenticatedContext.getParameter(Access.class);
        return OSFactory.clientFromAccess(access, Facing.value(facing)).identity().tenants().getByName(osCredential.getTenantName()).getId();
    }

    public boolean isV2Keystone(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        return CB_KEYSTONE_V2.equals(osCredential.getVersion());
    }

    public boolean isV2Keystone(CloudCredential cloudCredential) {
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);
        return CB_KEYSTONE_V2.equals(osCredential.getVersion());
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
                    if (type.isPresent()) {
                        return new Builder()
                                .name(r.getPhysicalResourceId())
                                .type(type.get())
                                .build();
                    }
                    return null;
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
            case OS_NEUTRON_ROUTER:
                result = ResourceType.OPENSTACK_ROUTER;
                break;
            default:
                LOGGER.debug("Not a valid resource type for OS: {}", resource.getType());
                break;
        }

        return Optional.ofNullable(result);
    }

    private Access createAccess(CloudCredential cloudCredential) {
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);

        if (CB_KEYSTONE_V2.equals(osCredential.getVersion())) {
            try {
                return OSFactory.builderV2().withConfig(config).endpoint(osCredential.getEndpoint())
                        .credentials(osCredential.getUserName(), osCredential.getPassword())
                        .tenantName(osCredential.getTenantName())
                        .authenticate()
                        .getAccess();
            } catch (AuthenticationException | ClientResponseException e) {
                LOGGER.info("Openstack authentication failed", e);
                throw new CredentialVerificationException("Authentication failed to openstack, message: " + e.getMessage(), e);
            }
        }
        return null;
    }

    private Token createToken(CloudCredential cloudCredential) {
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);
        if (osCredential == null || osCredential.getScope() == null) {
            return null;
        }
        switch (osCredential.getScope()) {
            case CB_KEYSTONE_V3_DEFAULT_SCOPE:
                return OSFactory.builderV3().withConfig(config).endpoint(osCredential.getEndpoint())
                        .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                        .authenticate()
                        .getToken();
            case CB_KEYSTONE_V3_DOMAIN_SCOPE:
                return OSFactory.builderV3().withConfig(config).endpoint(osCredential.getEndpoint())
                        .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                        .scopeToDomain(Identifier.byName(osCredential.getDomainName()))
                        .authenticate()
                        .getToken();
            case CB_KEYSTONE_V3_PROJECT_SCOPE:
                return OSFactory.builderV3().withConfig(config).endpoint(osCredential.getEndpoint())
                        .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                        .scopeToProject(Identifier.byName(osCredential.getProjectName()), Identifier.byName(osCredential.getProjectDomain()))
                        .authenticate()
                        .getToken();
            default:
                return null;
        }
    }

    private void createAccessOrToken(AuthenticatedContext authenticatedContext) {
        Access access = createAccess(authenticatedContext.getCloudCredential());
        Token token = createToken(authenticatedContext.getCloudCredential());

        if (token == null && access == null) {
            throw new CloudConnectorException("Unsupported keystone version");
        } else if (token != null) {
            authenticatedContext.putParameter(Token.class, token);
        } else {
            authenticatedContext.putParameter(Access.class, access);
        }
    }

    public Set<String> getRegion(CloudCredential cloudCredential) {
        Access access = createAccess(cloudCredential);
        Token token = createToken(cloudCredential);

        Set<String> regions = new HashSet<>();

        if (token == null && access == null) {
            throw new CloudConnectorException("Unsupported keystone version");
        } else if (token != null) {
            for (Service service : token.getCatalog()) {
                for (Endpoint endpoint : service.getEndpoints()) {
                    regions.add(endpoint.getRegion());
                }
            }
        } else {
            for (Access.Service service : access.getServiceCatalog()) {
                for (org.openstack4j.model.identity.v2.Endpoint endpoint : service.getEndpoints()) {
                    regions.add(endpoint.getRegion());
                }
            }
        }
        LOGGER.info("regions from openstack: {}", regions);
        return regions;
    }

    public List<com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone> getZones(OSClient<?> osClient, String regionFromOpenStack) {
        List<? extends AvailabilityZone> zonesFromOS = osClient.compute().zones().list();
        LOGGER.info("zones from openstack for {}: {}", regionFromOpenStack, zonesFromOS);
        return zonesFromOS.stream().map(z -> availabilityZone(z.getZoneName())).collect(Collectors.toList());
    }

    public List<? extends Flavor> getFlavors(OSClient<?> osClient) {
        return osClient.compute().flavors().list();
    }

}
