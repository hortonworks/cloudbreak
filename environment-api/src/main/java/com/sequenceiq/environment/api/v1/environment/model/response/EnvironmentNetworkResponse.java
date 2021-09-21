package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.EnvironmentNetworkBase;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkV1Response")
public class EnvironmentNetworkResponse extends EnvironmentNetworkBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private String crn;

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(value = EnvironmentModelDescription.SUBNET_METAS)
    private Map<String, CloudSubnet> subnetMetas;

    @ApiModelProperty(value = EnvironmentModelDescription.CB_SUBNETS)
    private Map<String, CloudSubnet> cbSubnets;

    @ApiModelProperty(value = EnvironmentModelDescription.DWX_SUBNETS)
    private Map<String, CloudSubnet> dwxSubnets;

    @ApiModelProperty(value = EnvironmentModelDescription.MLX_SUBNETS)
    private Map<String, CloudSubnet> mlxSubnets;

    @ApiModelProperty(value = EnvironmentModelDescription.CB_SUBNETS)
    private Map<String, CloudSubnet> liftieSubnets;

    @ApiModelProperty(value = EnvironmentModelDescription.EXISTING_NETWORK)
    private boolean existingNetwork;

    @ApiModelProperty(EnvironmentModelDescription.PREFERED_SUBNET_ID)
    private String preferedSubnetId;

    @ApiModelProperty(EnvironmentModelDescription.PREFERED_SUBNET_IDS)
    private Set<String> preferedSubnetIds;

    @ApiModelProperty(EnvironmentModelDescription.NETWORKCIDRS)
    private Set<String> networkCidrs = new HashSet<>();

    @ApiModelProperty(value = EnvironmentModelDescription.ENDPOINT_ACCESS_GATEWAY_SUBNET_METAS)
    private Map<String, CloudSubnet> gatewayEndpointSubnetMetas = Map.of();

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setCbSubnets(Map<String, CloudSubnet> cbSubnets) {
        this.cbSubnets = cbSubnets;
    }

    public Map<String, CloudSubnet> getDwxSubnets() {
        return dwxSubnets;
    }

    public void setDwxSubnets(Map<String, CloudSubnet> dwxSubnets) {
        this.dwxSubnets = dwxSubnets;
    }

    public Map<String, CloudSubnet> getMlxSubnets() {
        return mlxSubnets;
    }

    public void setMlxSubnets(Map<String, CloudSubnet> mlxSubnets) {
        this.mlxSubnets = mlxSubnets;
    }

    public Map<String, CloudSubnet> getLiftieSubnets() {
        return liftieSubnets;
    }

    public void setLiftieSubnets(Map<String, CloudSubnet> liftieSubnets) {
        this.liftieSubnets = liftieSubnets;
    }

    public boolean isExistingNetwork() {
        return existingNetwork;
    }

    public void setExistingNetwork(boolean existingNetwork) {
        this.existingNetwork = existingNetwork;
    }

    public String getPreferedSubnetId() {
        return preferedSubnetId;
    }

    public void setPreferedSubnetId(String preferedSubnetId) {
        this.preferedSubnetId = preferedSubnetId;
    }

    public Set<String> getPreferedSubnetIds() {
        return preferedSubnetIds;
    }

    public void setPreferedSubnetIds(Set<String> preferedSubnetIds) {
        this.preferedSubnetIds = preferedSubnetIds;
    }

    public Set<String> getNetworkCidrs() {
        return networkCidrs;
    }

    public void setNetworkCidrs(Set<String> networkCidrs) {
        this.networkCidrs = networkCidrs;
    }

    public Map<String, CloudSubnet> getGatewayEndpointSubnetMetas() {
        return gatewayEndpointSubnetMetas;
    }

    public void setGatewayEndpointSubnetMetas(Map<String, CloudSubnet> gatewayEndpointSubnetMetas) {
        this.gatewayEndpointSubnetMetas = gatewayEndpointSubnetMetas;
    }

    @Override
    public String toString() {
        return super.toString() + "EnvironmentNetworkResponse{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", subnetMetas=" + subnetMetas +
                ", cbSubnets=" + cbSubnets +
                ", dwxSubnets=" + dwxSubnets +
                ", mlxSubnets=" + mlxSubnets +
                ", liftieSubnets=" + liftieSubnets +
                ", existingNetwork=" + existingNetwork +
                ", preferedSubnetId='" + preferedSubnetId + '\'' +
                ", preferedSubnetIds='" + preferedSubnetIds + '\'' +
                ", networkCidrs=" + networkCidrs +
                ", gatewayEndpointSubnetMetas=" + gatewayEndpointSubnetMetas +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String crn;

        private String name;

        private Set<String> subnetIds;

        private String networkCidr;

        private Map<String, CloudSubnet> subnetMetas;

        private Map<String, CloudSubnet> cbSubnets;

        private Map<String, CloudSubnet> dwxSubnets;

        private Map<String, CloudSubnet> mlxSubnets;

        private Map<String, CloudSubnet> liftieSubnets;

        private boolean existingNetwork;

        private PrivateSubnetCreation privateSubnetCreation;

        private ServiceEndpointCreation serviceEndpointCreation;

        private OutboundInternetTraffic outboundInternetTraffic;

        private String preferedSubnetId;

        private Set<String> preferedSubnetIds;

        private Set<String> networkCidrs;

        private EnvironmentNetworkAwsParams aws;

        private EnvironmentNetworkAzureParams azure;

        private EnvironmentNetworkYarnParams yarn;

        private EnvironmentNetworkMockParams mock;

        private EnvironmentNetworkGcpParams gcp;

        private PublicEndpointAccessGateway publicEndpointAccessGateway;

        private Map<String, CloudSubnet> endpointGatewaySubnetMetas;

        private Set<String> endpointGatewaySubnetIds;

        private Builder() {
        }

        public Builder withCrn(String id) {
            this.crn = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
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

        public Builder withExistingNetwork(boolean existingNetwork) {
            this.existingNetwork = existingNetwork;
            return this;
        }

        public Builder withPreferedSubnetId(String preferedSubnetId) {
            this.preferedSubnetId = preferedSubnetId;
            return this;
        }

        public Builder withPreferedSubnetIds(Set<String> preferedSubnetIds) {
            this.preferedSubnetIds = preferedSubnetIds;
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

        public Builder withAws(EnvironmentNetworkAwsParams aws) {
            this.aws = aws;
            return this;
        }

        public Builder withAzure(EnvironmentNetworkAzureParams azure) {
            this.azure = azure;
            return this;
        }

        public Builder withYarn(EnvironmentNetworkYarnParams yarn) {
            this.yarn = yarn;
            return this;
        }

        public Builder withGcp(EnvironmentNetworkGcpParams gcp) {
            this.gcp = gcp;
            return this;
        }

        public Builder withNetworkCidr(String networkCidr) {
            this.networkCidr = networkCidr;
            return this;
        }

        public Builder withNetworkCidrs(Set<String> networkCidrs) {
            this.networkCidrs = networkCidrs;
            return this;
        }

        public Builder withMock(EnvironmentNetworkMockParams mock) {
            this.mock = mock;
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

        public Builder withEndpointGatewaySubnetIds(Set<String> endpointGatewaySubnetIds) {
            this.endpointGatewaySubnetIds = endpointGatewaySubnetIds;
            return this;
        }

        public EnvironmentNetworkResponse build() {
            EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
            environmentNetworkResponse.setCrn(crn);
            environmentNetworkResponse.setName(name);
            environmentNetworkResponse.setSubnetIds(subnetIds);
            environmentNetworkResponse.setNetworkCidr(networkCidr);
            environmentNetworkResponse.setNetworkCidrs(networkCidrs);
            environmentNetworkResponse.setAws(aws);
            environmentNetworkResponse.setAzure(azure);
            environmentNetworkResponse.setYarn(yarn);
            environmentNetworkResponse.setGcp(gcp);
            environmentNetworkResponse.setSubnetMetas(subnetMetas);
            environmentNetworkResponse.setExistingNetwork(existingNetwork);
            environmentNetworkResponse.setMock(mock);
            environmentNetworkResponse.setPreferedSubnetId(preferedSubnetId);
            environmentNetworkResponse.setPrivateSubnetCreation(privateSubnetCreation);
            environmentNetworkResponse.setServiceEndpointCreation(serviceEndpointCreation);
            environmentNetworkResponse.setOutboundInternetTraffic(outboundInternetTraffic);
            environmentNetworkResponse.setCbSubnets(cbSubnets);
            environmentNetworkResponse.setMlxSubnets(mlxSubnets);
            environmentNetworkResponse.setDwxSubnets(dwxSubnets);
            environmentNetworkResponse.setLiftieSubnets(liftieSubnets);
            environmentNetworkResponse.setPublicEndpointAccessGateway(publicEndpointAccessGateway);
            environmentNetworkResponse.setGatewayEndpointSubnetMetas(endpointGatewaySubnetMetas);
            environmentNetworkResponse.setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
            environmentNetworkResponse.setPreferedSubnetIds(preferedSubnetIds);
            return environmentNetworkResponse;
        }
    }

}
