package com.sequenceiq.environment.environment.dto.dataservices;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = EnvironmentDataServices.Builder.class)
public record EnvironmentDataServices(AwsDataServiceParameters aws, AzureDataServiceParameters azure, GcpDataServiceParameters gcp,
        CustomDockerRegistryParameters customDockerRegistry) implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private AwsDataServiceParameters aws;

        private AzureDataServiceParameters azure;

        private GcpDataServiceParameters gcp;

        private CustomDockerRegistryParameters customDockerRegistry;

        private Builder() {
        }

        public Builder withAws(AwsDataServiceParameters aws) {
            this.aws = aws;
            return this;
        }

        public Builder withAzure(AzureDataServiceParameters azure) {
            this.azure = azure;
            return this;
        }

        public Builder withGcp(GcpDataServiceParameters gcp) {
            this.gcp = gcp;
            return this;
        }

        public Builder withCustomDockerRegistry(CustomDockerRegistryParameters customDockerRegistry) {
            this.customDockerRegistry = customDockerRegistry;
            return this;
        }

        public EnvironmentDataServices build() {
            return new EnvironmentDataServices(aws, azure, gcp, customDockerRegistry);
        }
    }
}
