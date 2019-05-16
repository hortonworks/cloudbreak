package com.sequenceiq.environment.network;

public class NetworkDto {

    private Long id;

    private Long environmentId;

    private final String status;

    public NetworkDto(String status) {
        this.status = status;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getStatus() {
        return status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static final class NetworkDtoBuilder {
        private Long id;

        private Long environmentId;

        private String status;

        private NetworkDtoBuilder() {
        }

        public static NetworkDtoBuilder aVpcDto() {
            return new NetworkDtoBuilder();
        }

        public NetworkDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public NetworkDtoBuilder withEnvironmentId(Long environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public NetworkDtoBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public NetworkDto build() {
            NetworkDto networkDto = new NetworkDto(status);
            networkDto.setId(id);
            networkDto.setEnvironmentId(environmentId);
            return networkDto;
        }
    }
}
