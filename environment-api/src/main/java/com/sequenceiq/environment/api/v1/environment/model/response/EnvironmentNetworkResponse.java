package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.EnvironmentNetworkBase;

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

    @ApiModelProperty(value = EnvironmentModelDescription.EXISTING_NETWORK)
    private boolean existingNetwork;

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

    public boolean isExistingNetwork() {
        return existingNetwork;
    }

    public void setExistingNetwork(boolean existingNetwork) {
        this.existingNetwork = existingNetwork;
    }

    public static final class EnvironmentNetworkResponseBuilder {
        private String crn;

        private String name;

        private Set<String> subnetIds;

        private String networkCidr;

        private Map<String, CloudSubnet> subnetMetas;

        private boolean existingNetwork;

        private EnvironmentNetworkAwsParams aws;

        private EnvironmentNetworkAzureParams azure;

        private EnvironmentNetworkYarnParams yarn;

        private EnvironmentNetworkResponseBuilder() {
        }

        public static EnvironmentNetworkResponseBuilder anEnvironmentNetworkResponse() {
            return new EnvironmentNetworkResponseBuilder();
        }

        public EnvironmentNetworkResponseBuilder withCrn(String id) {
            this.crn = id;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withSubnetIds(Set<String> subnetIds) {
            this.subnetIds = subnetIds;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withSubnetMetas(Map<String, CloudSubnet> subnetMetas) {
            this.subnetMetas = subnetMetas;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withExistingNetwork(boolean existingNetwork) {
            this.existingNetwork = existingNetwork;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withAws(EnvironmentNetworkAwsParams aws) {
            this.aws = aws;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withAzure(EnvironmentNetworkAzureParams azure) {
            this.azure = azure;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withYarn(EnvironmentNetworkYarnParams yarn) {
            this.yarn = yarn;
            return this;
        }

        public EnvironmentNetworkResponseBuilder withNetworkCidr(String networkCidr) {
            this.networkCidr = networkCidr;
            return this;
        }

        public EnvironmentNetworkResponse build() {
            EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
            environmentNetworkResponse.setCrn(crn);
            environmentNetworkResponse.setName(name);
            environmentNetworkResponse.setSubnetIds(subnetIds);
            environmentNetworkResponse.setNetworkCidr(networkCidr);
            environmentNetworkResponse.setAws(aws);
            environmentNetworkResponse.setAzure(azure);
            environmentNetworkResponse.setYarn(yarn);
            environmentNetworkResponse.setSubnetMetas(subnetMetas);
            environmentNetworkResponse.setExistingNetwork(existingNetwork);
            return environmentNetworkResponse;
        }
    }
}
