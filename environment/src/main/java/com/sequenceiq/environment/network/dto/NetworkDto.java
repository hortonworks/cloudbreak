package com.sequenceiq.environment.network.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

public class NetworkDto {

    private Long id;

    private String name;

    private final String resourceCrn;

    private final AwsParams aws;

    private final AzureParams azure;

    private final Set<String> subnetIds;

    private final String networkCidr;

    private final Map<String, CloudSubnet> subnetMetas;

    public NetworkDto(Builder builder) {
        this.id = builder.id;
        this.resourceCrn = builder.resourceCrn;
        this.name = builder.name;
        this.aws = builder.aws;
        this.azure = builder.azure;
        this.subnetIds = CollectionUtils.isEmpty(builder.subnetIds) ? new HashSet<>() : builder.subnetIds;
        this.subnetMetas = MapUtils.isEmpty(builder.subnetMetas) ? new HashMap<>() : builder.subnetMetas;
        this.networkCidr = builder.networkCidr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNetworkName() {
        return name;
    }

    public String getResourceCrn() {
        return resourceCrn;
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

    public String getNetworkCidr() {
        return networkCidr;
    }

    public Map<String, CloudSubnet> getSubnetMetas() {
        return subnetMetas;
    }

    public static final class Builder {
        private Long id;

        private String name;

        private String resourceCrn;

        private AwsParams aws;

        private AzureParams azure;

        private Set<String> subnetIds;

        private Map<String, CloudSubnet> subnetMetas;

        private String networkCidr;

        private Builder() {
        }

        public static Builder aNetworkDto() {
            return new Builder();
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAws(AwsParams aws) {
            this.aws = aws;
            return this;
        }

        public Builder withAzure(AzureParams azure) {
            this.azure = azure;
            return this;
        }

        public Builder withSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public Builder withSubnetMetas(Map<String, CloudSubnet> subnetMetas) {
            this.subnetMetas = subnetMetas;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withNetworkCidr(String networkCidr) {
            this.networkCidr = networkCidr;
            return this;
        }

        public NetworkDto build() {
            return new NetworkDto(this);
        }
    }
}
