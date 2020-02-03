package com.sequenceiq.environment.network.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;

public class NetworkDto {

    private Long id;

    private final String name;

    private final String networkId;

    private final String resourceCrn;

    private final AwsParams aws;

    private final AzureParams azure;

    private final YarnParams yarn;

    private final MockParams mock;

    private final String networkCidr;

    private Map<String, CloudSubnet> subnetMetas;

    private final PrivateSubnetCreation privateSubnetCreation;

    private final RegistrationType registrationType;

    private final CloudPlatform cloudPlatform;

    public NetworkDto(Builder builder) {
        this.id = builder.id;
        this.resourceCrn = builder.resourceCrn;
        this.name = builder.name;
        this.aws = builder.aws;
        this.azure = builder.azure;
        this.yarn = builder.yarn;
        this.mock = builder.mock;
        this.subnetMetas = MapUtils.isEmpty(builder.subnetMetas) ? new HashMap<>() : builder.subnetMetas;
        this.networkCidr = builder.networkCidr;
        this.networkId = builder.networkId;
        this.privateSubnetCreation = builder.privateSubnetCreation;
        this.registrationType = builder.registrationType;
        this.cloudPlatform = builder.cloudPlatform;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(NetworkDto networkDto) {
        return new Builder(networkDto);
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

    public YarnParams getYarn() {
        return yarn;
    }

    public MockParams getMock() {
        return mock;
    }

    public Set<String> getSubnetIds() {
        return subnetMetas.keySet();
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public Map<String, CloudSubnet> getSubnetMetas() {
        return subnetMetas;
    }

    public void setSubnetMetas(Map<String, CloudSubnet> subnetMetas) {
        this.subnetMetas = subnetMetas;
    }

    public String getName() {
        return name;
    }

    public String getNetworkId() {
        return networkId;
    }

    public PrivateSubnetCreation getPrivateSubnetCreation() {
        return privateSubnetCreation;
    }

    public RegistrationType getRegistrationType() {
        return registrationType;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    @Override
    public String toString() {
        return "NetworkDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", networkId='" + networkId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", aws=" + aws +
                ", azure=" + azure +
                ", yarn=" + yarn +
                ", mock=" + mock +
                ", networkCidr='" + networkCidr + '\'' +
                ", subnetMetas=" + subnetMetas +
                ", privateSubnetCreation=" + privateSubnetCreation +
                ", registrationType=" + registrationType +
                ", cloudPlatform=" + cloudPlatform +
                '}';
    }

    public static final class Builder {

        private Long id;

        private String name;

        private String networkId;

        private String resourceCrn;

        private AwsParams aws;

        private AzureParams azure;

        private YarnParams yarn;

        private MockParams mock;

        private Map<String, CloudSubnet> subnetMetas;

        private String networkCidr;

        private PrivateSubnetCreation privateSubnetCreation;

        private RegistrationType registrationType;

        private CloudPlatform cloudPlatform;

        private Builder() {
        }

        private Builder(NetworkDto networkDto) {
            this.id = networkDto.id;
            this.name = networkDto.name;
            this.networkId = networkDto.networkId;
            this.resourceCrn = networkDto.resourceCrn;
            this.aws = networkDto.aws;
            this.azure = networkDto.azure;
            this.yarn = networkDto.yarn;
            this.mock = networkDto.mock;
            this.subnetMetas = networkDto.subnetMetas;
            this.networkCidr = networkDto.networkCidr;
            this.privateSubnetCreation = networkDto.privateSubnetCreation;
            this.registrationType = networkDto.registrationType;
            this.cloudPlatform = networkDto.cloudPlatform;
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
            cloudPlatform = CloudPlatform.AWS;
            return this;
        }

        public Builder withAzure(AzureParams azure) {
            this.azure = azure;
            cloudPlatform = CloudPlatform.AZURE;
            return this;
        }

        public Builder withYarn(YarnParams yarn) {
            this.yarn = yarn;
            cloudPlatform = CloudPlatform.YARN;
            return this;
        }

        public Builder withMock(MockParams mock) {
            this.mock = mock;
            cloudPlatform = CloudPlatform.MOCK;
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

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withPrivateSubnetCreation(PrivateSubnetCreation privateSubnetCreation) {
            this.privateSubnetCreation = privateSubnetCreation;
            return this;
        }

        public Builder withRegistrationType(RegistrationType registrationType) {
            this.registrationType = registrationType;
            return this;
        }

        public NetworkDto build() {
            return new NetworkDto(this);
        }
    }
}
