package com.sequenceiq.environment.network.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
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

    private final GcpParams gcp;

    private final String networkCidr;

    private final Set<String> networkCidrs;

    private Map<String, CloudSubnet> subnetMetas;

    private PublicEndpointAccessGateway publicEndpointAccessGateway;

    private Map<String, CloudSubnet> endpointGatewaySubnetMetas;

    private final Map<String, CloudSubnet> cbSubnets;

    private final Map<String, CloudSubnet> dwxSubnets;

    private final Map<String, CloudSubnet> mlxSubnets;

    private final Map<String, CloudSubnet> liftieSubnets;

    private final PrivateSubnetCreation privateSubnetCreation;

    private final ServiceEndpointCreation serviceEndpointCreation;

    private final OutboundInternetTraffic outboundInternetTraffic;

    private final RegistrationType registrationType;

    private final CloudPlatform cloudPlatform;

    private NetworkDto(Builder builder) {
        id = builder.id;
        resourceCrn = builder.resourceCrn;
        name = builder.name;
        aws = builder.aws;
        azure = builder.azure;
        yarn = builder.yarn;
        mock = builder.mock;
        subnetMetas = MapUtils.isEmpty(builder.subnetMetas) ? new HashMap<>() : builder.subnetMetas;
        publicEndpointAccessGateway = builder.publicEndpointAccessGateway;
        endpointGatewaySubnetMetas = MapUtils.isEmpty(builder.endpointGatewaySubnetMetas) ?
            new HashMap<>() : builder.endpointGatewaySubnetMetas;
        cbSubnets = builder.cbSubnets;
        dwxSubnets = builder.dwxSubnets;
        mlxSubnets = builder.mlxSubnets;
        liftieSubnets = builder.liftieSubnets;
        networkCidr = builder.networkCidr;
        networkCidrs = builder.networkCidrs;
        networkId = builder.networkId;
        gcp = builder.gcp;
        privateSubnetCreation = builder.privateSubnetCreation;
        serviceEndpointCreation = builder.serviceEndpointCreation;
        outboundInternetTraffic = builder.outboundInternetTraffic;
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

    public GcpParams getGcp() {
        return gcp;
    }

    public Set<String> getSubnetIds() {
        return subnetMetas != null ? subnetMetas.keySet() : new HashSet<>();
    }

    public Set<String> getEndpointGatewaySubnetIds() {
        return endpointGatewaySubnetMetas != null ? endpointGatewaySubnetMetas.keySet() : new HashSet<>();
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public Set<String> getNetworkCidrs() {
        return networkCidrs;
    }

    public Map<String, CloudSubnet> getSubnetMetas() {
        return subnetMetas;
    }

    public void setSubnetMetas(Map<String, CloudSubnet> subnetMetas) {
        this.subnetMetas = subnetMetas;
    }

    public PublicEndpointAccessGateway getPublicEndpointAccessGateway() {
        return publicEndpointAccessGateway;
    }

    public void setPublicEndpointAccessGateway(PublicEndpointAccessGateway publicEndpointAccessGateway) {
        this.publicEndpointAccessGateway = publicEndpointAccessGateway;
    }

    public Map<String, CloudSubnet> getEndpointGatewaySubnetMetas() {
        return endpointGatewaySubnetMetas;
    }

    public void setEndpointGatewaySubnetMetas(Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        this.endpointGatewaySubnetMetas = endpointGatewaySubnetMetas;
    }

    public Map<String, CloudSubnet> getCbSubnets() {
        return cbSubnets;
    }

    public Collection<CloudSubnet> getCbSubnetValues() {
        return cbSubnets != null ? cbSubnets.values() : new ArrayList<>();
    }

    public Map<String, CloudSubnet> getDwxSubnets() {
        return dwxSubnets;
    }

    public Map<String, CloudSubnet> getMlxSubnets() {
        return mlxSubnets;
    }

    public Map<String, CloudSubnet> getLiftieSubnets() {
        return liftieSubnets;
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

    public ServiceEndpointCreation getServiceEndpointCreation() {
        return serviceEndpointCreation;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
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
                ", gcp=" + gcp +
                ", yarn=" + yarn +
                ", mock=" + mock +
                ", networkCidr='" + networkCidr + '\'' +
                ", networkCidrs='" + networkCidrs + '\'' +
                ", subnetMetas=" + subnetMetas +
                ", publicEndpointAccessGateway=" + publicEndpointAccessGateway +
                ", endpointGatewaySubnetMetas=" + endpointGatewaySubnetMetas +
                ", privateSubnetCreation=" + privateSubnetCreation +
                ", serviceEndpointCreation=" + serviceEndpointCreation +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
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

        private GcpParams gcp;

        private MockParams mock;

        private Map<String, CloudSubnet> subnetMetas;

        private PublicEndpointAccessGateway publicEndpointAccessGateway;

        private Map<String, CloudSubnet> endpointGatewaySubnetMetas;

        private Map<String, CloudSubnet> cbSubnets;

        private Map<String, CloudSubnet> dwxSubnets;

        private Map<String, CloudSubnet> mlxSubnets;

        private Map<String, CloudSubnet> liftieSubnets;

        private String networkCidr;

        private Set<String> networkCidrs;

        private PrivateSubnetCreation privateSubnetCreation;

        private ServiceEndpointCreation serviceEndpointCreation;

        private OutboundInternetTraffic outboundInternetTraffic;

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
            publicEndpointAccessGateway = networkDto.publicEndpointAccessGateway;
            endpointGatewaySubnetMetas = networkDto.endpointGatewaySubnetMetas;
            networkCidr = networkDto.networkCidr;
            privateSubnetCreation = networkDto.privateSubnetCreation;
            serviceEndpointCreation = networkDto.serviceEndpointCreation;
            outboundInternetTraffic = networkDto.outboundInternetTraffic;
            registrationType = networkDto.registrationType;
            cloudPlatform = networkDto.cloudPlatform;
            cbSubnets = networkDto.cbSubnets;
            mlxSubnets = networkDto.mlxSubnets;
            dwxSubnets = networkDto.dwxSubnets;
            liftieSubnets = networkDto.liftieSubnets;
            networkCidrs = networkDto.networkCidrs;
            gcp = networkDto.gcp;
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

        public Builder withGcp(GcpParams gcp) {
            this.gcp = gcp;
            cloudPlatform = CloudPlatform.GCP;
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

        public Builder withUsePublicEndpointAccessGateway(PublicEndpointAccessGateway publicEndpointAccessGateway) {
            this.publicEndpointAccessGateway = publicEndpointAccessGateway;
            return this;
        }

        public Builder withEndpointGatewaySubnetMetas(Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
            this.endpointGatewaySubnetMetas = endpointGatewaySubnetMetas;
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

        public Builder withLiftieSubnets(Map<String, CloudSubnet> liftieSubnets) {
            this.liftieSubnets = liftieSubnets;
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

        public Builder withServiceEndpointCreation(ServiceEndpointCreation serviceEndpointCreation) {
            this.serviceEndpointCreation = serviceEndpointCreation;
            return this;
        }

        public Builder withOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
            this.outboundInternetTraffic = outboundInternetTraffic;
            return this;
        }

        public Builder withRegistrationType(RegistrationType registrationType) {
            this.registrationType = registrationType;
            return this;
        }

        public Builder withNetworkCidrs(Set<String> networkCidrs) {
            this.networkCidrs = networkCidrs;
            return this;
        }

        public NetworkDto build() {
            return new NetworkDto(this);
        }
    }
}
