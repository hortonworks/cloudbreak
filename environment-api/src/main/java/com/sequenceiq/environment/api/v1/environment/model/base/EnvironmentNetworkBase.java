package com.sequenceiq.environment.api.v1.environment.model.base;

import java.util.Set;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.SubnetType;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.validator.cidr.NetworkCidr;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(subTypes = {EnvironmentNetworkRequest.class, EnvironmentNetworkResponse.class})
public abstract class EnvironmentNetworkBase {

    @ApiModelProperty(value = EnvironmentModelDescription.SUBNET_IDS)
    private Set<String> subnetIds;

    @Size(max = 255)
    @NetworkCidr
    @ValidSubnet(SubnetType.RFC_1918_COMPLIANT_ONLY_OR_EMPTY)
    private String networkCidr;

    @ApiModelProperty(EnvironmentModelDescription.PRIVATE_SUBNET_CREATION)
    private PrivateSubnetCreation privateSubnetCreation = PrivateSubnetCreation.DISABLED;

    @ApiModelProperty(EnvironmentModelDescription.SERVICE_ENDPOINT_CREATION)
    private ServiceEndpointCreation serviceEndpointCreation = ServiceEndpointCreation.DISABLED;

    @ApiModelProperty(EnvironmentModelDescription.OUTBOUND_INTERNET_TRAFFIC)
    private OutboundInternetTraffic outboundInternetTraffic = OutboundInternetTraffic.ENABLED;

    @ApiModelProperty(EnvironmentModelDescription.AWS_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAwsParams aws;

    @ApiModelProperty(EnvironmentModelDescription.AZURE_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkAzureParams azure;

    @ApiModelProperty(EnvironmentModelDescription.YARN_SPECIFIC_PARAMETERS)
    private EnvironmentNetworkYarnParams yarn;

    @ApiModelProperty(EnvironmentModelDescription.MOCK_PARAMETERS)
    private EnvironmentNetworkMockParams mock;

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public PrivateSubnetCreation getPrivateSubnetCreation() {
        return privateSubnetCreation;
    }

    public void setPrivateSubnetCreation(PrivateSubnetCreation privateSubnetCreation) {
        this.privateSubnetCreation = privateSubnetCreation;
    }

    public ServiceEndpointCreation getServiceEndpointCreation() {
        return serviceEndpointCreation;
    }

    public void setServiceEndpointCreation(ServiceEndpointCreation serviceEndpointCreation) {
        this.serviceEndpointCreation = serviceEndpointCreation;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public void setOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public EnvironmentNetworkAwsParams getAws() {
        return aws;
    }

    public void setAws(EnvironmentNetworkAwsParams aws) {
        this.aws = aws;
    }

    public EnvironmentNetworkAzureParams getAzure() {
        return azure;
    }

    public void setAzure(EnvironmentNetworkAzureParams azure) {
        this.azure = azure;
    }

    public EnvironmentNetworkYarnParams getYarn() {
        return yarn;
    }

    public void setYarn(EnvironmentNetworkYarnParams yarn) {
        this.yarn = yarn;
    }

    public EnvironmentNetworkMockParams getMock() {
        return mock;
    }

    public void setMock(EnvironmentNetworkMockParams mock) {
        this.mock = mock;
    }

}
