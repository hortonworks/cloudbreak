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

    private final Map<String, CloudSubnet> cbSubnets;

    private final Map<String, CloudSubnet> dwxSubnets;

    private final Map<String, CloudSubnet> mlxSubnets;

    private final PrivateSubnetCreation privateSubnetCreation;

    private final RegistrationType registrationType;

    private final CloudPlatform cloudPlatform;

    public NetworkDto(Builder builder) {
        id = builder.id;
        resourceCrn = builder.resourceCrn;
        name = builder.name;
        aws = builder.aws;
        azure = builder.azure;
        yarn = builder.yarn;
        mock = builder.mock;
        subnetMetas = MapUtils.isEmpty(builder.subnetMetas) ? new HashMap<>() : builder.subnetMetas;
        cbSubnets = builder.cbSubnets;
        dwxSubnets = builder.dwxSubnets;
        mlxSubnets = builder.mlxSubnets;
        networkCidr = builder.networkCidr;
        networkId = builder.networkId;
        privateSubnetCreation = builder.privateSubnetCreation;
        registrationType = builder.registrationType;
        cloudPlatform = builder.cloudPlatform;
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

    public Map<String, CloudSubnet> getCbSubnets() {
        return cbSubnets;
    }

    public Map<String, CloudSubnet> getDwxSubnets() {
        return dwxSubnets;
    }

    public Map<String, CloudSubnet> getMlxSubnets() {
        return mlxSubnets;
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

        private Map<String, CloudSubnet> cbSubnets;

        private Map<String, CloudSubnet> dwxSubnets;

        private Map<String, CloudSubnet> mlxSubnets;

        private String networkCidr;

        private PrivateSubnetCreation privateSubnetCreation;

        private RegistrationType registrationType;

        private CloudPlatform cloudPlatform;

        private Builder() {
        }

        private Builder(NetworkDto networkDto) {
            id = networkDto.id;
            name = networkDto.name;
            networkId = networkDto.networkId;
            resourceCrn = networkDto.resourceCrn;
            aws = networkDto.aws;
            azure = networkDto.azure;
            yarn = networkDto.yarn;
            mock = networkDto.mock;
            subnetMetas = networkDto.subnetMetas;
            networkCidr = networkDto.networkCidr;
            privateSubnetCreation = networkDto.privateSubnetCreation;
            registrationType = networkDto.registrationType;
            cloudPlatform = networkDto.cloudPlatform;
            cbSubnets = networkDto.cbSubnets;
            mlxSubnets = networkDto.mlxSubnets;
            dwxSubnets = networkDto.dwxSubnets;
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

        public Builder withCbSubnets(Map<String, CloudSubnet> cbSubnets) {
            this.cbSubnets = cbSubnets;
            return this;
        }

        public Builder withDwxSubnets(Map<String, CloudSubnet> dwxSubnets) {
            this.dwxSubnets = dwxSubnets;
            return this;
        }

        public Builder withMlxSubnets(Map<String, CloudSubnet> mlxSubnets) {
            this.mlxSubnets = mlxSubnets;
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
