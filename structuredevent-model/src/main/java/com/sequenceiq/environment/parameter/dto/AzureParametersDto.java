package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AzureParametersDto.Builder.class)
public class AzureParametersDto {

    private final AzureResourceGroupDto azureResourceGroupDto;

    private final AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

    private final boolean noOutboundLoadBalancer;

    private AzureParametersDto(Builder builder) {
        azureResourceGroupDto = builder.azureResourceGroupDto;
        azureResourceEncryptionParametersDto = builder.azureResourceEncryptionParametersDto;
        noOutboundLoadBalancer = builder.noOutboundLoadBalancer;
    }

    public AzureResourceGroupDto getAzureResourceGroupDto() {
        return azureResourceGroupDto;
    }

    public AzureResourceEncryptionParametersDto getAzureResourceEncryptionParametersDto() {
        return azureResourceEncryptionParametersDto;
    }

    public boolean isNoOutboundLoadBalancer() {
        return noOutboundLoadBalancer;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureParametersDto{"
                + "azureResourceGroupDto=" + azureResourceGroupDto
                + ", azureResourceEncryptionParametersDto=" + azureResourceEncryptionParametersDto
                + ", isNoOutboundLoadBalancer=" + noOutboundLoadBalancer
                + '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private AzureResourceGroupDto azureResourceGroupDto;

        private AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto;

        private boolean noOutboundLoadBalancer;

        public Builder withAzureResourceGroupDto(AzureResourceGroupDto azureResourceGroupDto) {
            this.azureResourceGroupDto = azureResourceGroupDto;
            return this;
        }

        public Builder withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto azureResourceEncryptionParametersDto) {
            this.azureResourceEncryptionParametersDto = azureResourceEncryptionParametersDto;
            return this;
        }

        public Builder withNoOutboundLoadBalancer(boolean noOutboundLoadBalancer) {
            this.noOutboundLoadBalancer = noOutboundLoadBalancer;
            return this;
        }

        public AzureParametersDto build() {
            return new AzureParametersDto(this);
        }
    }
}
