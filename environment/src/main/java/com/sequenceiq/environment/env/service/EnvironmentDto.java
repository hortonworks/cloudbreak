package com.sequenceiq.environment.env.service;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.environment.network.NetworkDto;

public class EnvironmentDto implements Payload {

    private Long id;

    private NetworkDto networkDto;

    private final String name;

    private final String cloudPlatform;

    private final EnvironmentStatus status;

    public EnvironmentDto(String name, String cloudPlatform, EnvironmentStatus status) {
        this.name = name;
        this.cloudPlatform = cloudPlatform;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public NetworkDto getNetworkDto() {
        return networkDto;
    }

    public EnvironmentStatus getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNetworkDto(NetworkDto networkDto) {
        this.networkDto = networkDto;
    }

    @Override
    public Long getResourceId() {
        return id;
    }

    public static final class EnvironmentDtoBuilder {
        private Long id;

        private String name;

        private String cloudPlatform;

        private NetworkDto networkDto;

        private EnvironmentStatus status;

        private EnvironmentDtoBuilder() {
        }

        public static EnvironmentDtoBuilder anEnvironmentDto() {
            return new EnvironmentDtoBuilder();
        }

        public EnvironmentDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public EnvironmentDtoBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentDtoBuilder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public EnvironmentDtoBuilder withVpcDto(NetworkDto networkDto) {
            this.networkDto = networkDto;
            return this;
        }

        public EnvironmentDtoBuilder withStatus(EnvironmentStatus status) {
            this.status = status;
            return this;
        }

        public EnvironmentDto build() {
            EnvironmentDto environmentDto = new EnvironmentDto(name, cloudPlatform, status);
            environmentDto.setId(id);
            environmentDto.setNetworkDto(networkDto);
            return environmentDto;
        }
    }
}
