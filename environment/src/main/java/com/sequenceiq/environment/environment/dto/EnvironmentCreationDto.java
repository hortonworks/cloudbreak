package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.CredentialAwareEnvRequest;
import com.sequenceiq.environment.network.dto.NetworkDto;

public class EnvironmentCreationDto {

    private final String name;

    private final String description;

    private final String cloudPlatform;

    private final String accountId;

    private final LocationDto location;

    private final NetworkDto network;

    private final CredentialAwareEnvRequest credential;

    private final Set<String> regions;

    private final Set<String> proxyNames;

    private final boolean createFreeIpa;

    private final AuthenticationDto authentication;

    private final Telemetry telemetry;

    private final Long created;

    private final SecurityAccessDto securityAccess;

    private final Tunnel tunnel;

    private final IdBrokerMappingSource idBrokerMappingSource;

    //CHECKSTYLE:OFF
    public EnvironmentCreationDto(String name, String description, String cloudPlatform, String accountId,
            LocationDto location, NetworkDto network, CredentialAwareEnvRequest credential,
            Set<String> regions, Set<String> proxyNames, boolean createFreeIpa, AuthenticationDto authentication,
            Long created, Telemetry telemetry, SecurityAccessDto securityAccess, Tunnel tunnel, IdBrokerMappingSource idBrokerMappingSource) {
        //CHECKSTYLE:ON
        this.name = name;
        this.description = description;
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
        this.location = location;
        this.network = network;
        this.credential = credential;
        this.createFreeIpa = createFreeIpa;
        this.created = created;
        this.tunnel = tunnel;
        if (CollectionUtils.isEmpty(regions)) {
            this.regions = new HashSet<>();
        } else {
            this.regions = regions;
        }
        if (CollectionUtils.isEmpty(proxyNames)) {
            this.proxyNames = new HashSet<>();
        } else {
            this.proxyNames = proxyNames;
        }
        this.authentication = authentication;
        this.telemetry = telemetry;
        this.securityAccess = securityAccess;
        this.idBrokerMappingSource = idBrokerMappingSource;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public NetworkDto getNetwork() {
        return network;
    }

    public LocationDto getLocation() {
        return location;
    }

    public Set<String> getProxyNames() {
        return proxyNames;
    }

    public String getAccountId() {
        return accountId;
    }

    public CredentialAwareEnvRequest getCredential() {
        return credential;
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public boolean isCreateFreeIpa() {
        return createFreeIpa;
    }

    public AuthenticationDto getAuthentication() {
        return authentication;
    }

    public Long getCreated() {
        return created;
    }

    public SecurityAccessDto getSecurityAccess() {
        return securityAccess;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public IdBrokerMappingSource getIdBrokerMappingSource() {
        return idBrokerMappingSource;
    }

    public static final class Builder {
        private String name;

        private String description;

        private String cloudPlatform;

        private String accountId;

        private LocationDto location;

        private NetworkDto network;

        private CredentialAwareEnvRequest credential;

        private Set<String> regions;

        private Set<String> proxyNames;

        private Telemetry telemetry;

        private boolean createFreeIpa = true;

        private AuthenticationDto authentication;

        private Long created;

        private SecurityAccessDto securityAccess;

        private Tunnel tunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

        public Builder() {
        }

        public static Builder anEnvironmentCreationDto() {
            return new Builder();
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withLocation(LocationDto location) {
            this.location = location;
            return this;
        }

        public Builder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public Builder withCredential(CredentialAwareEnvRequest credential) {
            this.credential = credential;
            return this;
        }

        public Builder withRegions(Set<String> regions) {
            this.regions = regions;
            return this;
        }

        public Builder withProxyNames(Set<String> proxyNames) {
            this.proxyNames = proxyNames;
            return this;
        }

        public Builder withCreateFreeIpa(boolean createFreeIpa) {
            this.createFreeIpa = createFreeIpa;
            return this;
        }

        public Builder withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withTelemetry(Telemetry telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public Builder withSecurityAccess(SecurityAccessDto securityAccess) {
            this.securityAccess = securityAccess;
            return this;
        }

        public Builder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return this;
        }

        public EnvironmentCreationDto build() {
            return new EnvironmentCreationDto(name, description, cloudPlatform, accountId,
                    location, network, credential, regions, proxyNames, createFreeIpa,
                    authentication, created, telemetry, securityAccess, tunnel, idBrokerMappingSource);
        }
    }
}
