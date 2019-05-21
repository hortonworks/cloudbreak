package com.sequenceiq.environment.network.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;


public class NetworkDto {

    private final Long id;

    private final AwsParams aws;

    private final AzureParams azure;

    private final Set<String> subnetIds;

    public NetworkDto(Long id, AwsParams aws, AzureParams azure, Set<String> subnetIds) {
        this.id = id;
        this.aws = aws;
        this.azure = azure;
        if (CollectionUtils.isEmpty(subnetIds)) {
            this.subnetIds = new HashSet<>();
        } else {
            this.subnetIds = subnetIds;
        }
    }

    public Long getId() {
        return id;
    }

    public AwsParams getAws() {
        return aws;
    }

    public AzureParams getAzure() {
        return azure;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public static final class NetworkDtoBuilder {
        private Long id;

        private AwsParams aws;

        private AzureParams azure;

        private Set<String> subnetIds;

        private NetworkDtoBuilder() {
        }

        public static NetworkDtoBuilder aNetworkDto() {
            return new NetworkDtoBuilder();
        }

        public NetworkDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public NetworkDtoBuilder withAws(AwsParams aws) {
            this.aws = aws;
            return this;
        }

        public NetworkDtoBuilder withAzure(AzureParams azure) {
            this.azure = azure;
            return this;
        }

        public NetworkDtoBuilder withSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public NetworkDto build() {
            return new NetworkDto(id, aws, azure, subnetIds);
        }
    }
}
