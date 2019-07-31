package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

public class EnvironmentEditDto {

    private final String description;

    private final Set<String> regions;

    private final String accountId;

    private final EnvironmentTelemetry telemetry;

    private LocationDto location;

    private final NetworkDto network;

    private final AuthenticationDto authentication;

    private final SecurityAccessDto securityAccess;

    private final Tunnel tunnel;

    private final IdBrokerMappingSource idBrokerMappingSource;

    private final ParametersDto parameters;

    public EnvironmentEditDto(
            String description,
            Set<String> regions,
            String accountId,
            LocationDto location,
            NetworkDto network,
            AuthenticationDto authentication,
            EnvironmentTelemetry telemetry,
            SecurityAccessDto securityAccess,
            Tunnel tunnel,
            IdBrokerMappingSource idBrokerMappingSource,
            ParametersDto parameters) {
        this.description = description;
        this.accountId = accountId;
        if (CollectionUtils.isEmpty(regions)) {
            this.regions = new HashSet<>();
        } else {
            this.regions = regions;
        }
        this.network = network;
        this.location = location;
        this.authentication = authentication;
        this.telemetry = telemetry;
        this.securityAccess = securityAccess;
        this.tunnel = tunnel;
        this.idBrokerMappingSource = idBrokerMappingSource;
        this.parameters = parameters;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public String getAccountId() {
        return accountId;
    }

    public NetworkDto getNetworkDto() {
        return network;
    }

    public AuthenticationDto getAuthentication() {
        return authentication;
    }

    public EnvironmentTelemetry getTelemetry() {
        return telemetry;
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

    public ParametersDto getParameters() {
        return parameters;
    }

    public static final class EnvironmentEditDtoBuilder {
        private String description;

        private Set<String> regions;

        private String accountId;

        private LocationDto location;

        private NetworkDto network;

        private AuthenticationDto authentication;

        private EnvironmentTelemetry telemetry;

        private SecurityAccessDto securityAccess;

        private Tunnel tunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

        private ParametersDto parameters;

        private EnvironmentEditDtoBuilder() {
        }

        public static EnvironmentEditDtoBuilder anEnvironmentEditDto() {
            return new EnvironmentEditDtoBuilder();
        }

        public EnvironmentEditDtoBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentEditDtoBuilder withRegions(Set<String> regions) {
            this.regions = regions;
            return this;
        }

        public EnvironmentEditDtoBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public EnvironmentEditDtoBuilder withLocation(LocationDto location) {
            this.location = location;
            return this;
        }

        public EnvironmentEditDtoBuilder withNetwork(NetworkDto network) {
            this.network = network;
            return this;
        }

        public EnvironmentEditDtoBuilder withAuthentication(AuthenticationDto authentication) {
            this.authentication = authentication;
            return this;
        }

        public EnvironmentEditDtoBuilder withTelemetry(EnvironmentTelemetry telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public EnvironmentEditDtoBuilder withSecurityAccess(SecurityAccessDto securityAccess) {
            this.securityAccess = securityAccess;
            return this;
        }

        public EnvironmentEditDtoBuilder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public EnvironmentEditDtoBuilder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return this;
        }

        public EnvironmentEditDtoBuilder withParameters(ParametersDto parameters) {
            this.parameters = parameters;
            return this;
        }

        public EnvironmentEditDto build() {
            return new EnvironmentEditDto(description, regions, accountId, location, network,
                    authentication, telemetry, securityAccess, tunnel, idBrokerMappingSource, parameters);
        }
    }
}
