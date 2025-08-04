package com.sequenceiq.it.cloudbreak.dto.environment;

import java.util.Set;

import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class EnvironmentNetworkTestDto extends AbstractCloudbreakTestDto<EnvironmentNetworkRequest, EnvironmentNetworkResponse, EnvironmentNetworkTestDto> {
    public static final String ENV_NETWORK = "ENV_NETWORK";

    public EnvironmentNetworkTestDto(EnvironmentNetworkRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public EnvironmentNetworkTestDto(TestContext testContext) {
        super(new EnvironmentNetworkRequest(), testContext);
    }

    public EnvironmentNetworkTestDto valid() {
        return getCloudProvider()
                .network(this);
    }

    public EnvironmentNetworkTestDto withAzure(EnvironmentNetworkAzureParams azure) {
        getRequest().setAzure(azure);
        return this;
    }

    /**
     * Temporarily disable private endpoint setup for Azure Flexible Server as stop/start functionality is not working properly
     * and somtimes after flexible server start the server cannot be reached from the CM server node in case of private setup.
     */
    public EnvironmentNetworkTestDto disableAzurePrivateEndpoint() {
        EnvironmentNetworkAzureParams environmentNetworkAzureParams = getRequest().getAzure();
        if (environmentNetworkAzureParams != null) {
            environmentNetworkAzureParams.setDatabasePrivateDnsZoneId(null);
            withServiceEndpoints(ServiceEndpointCreation.DISABLED);
        }
        return this;
    }

    public EnvironmentNetworkTestDto withAws(EnvironmentNetworkAwsParams aws) {
        getRequest().setAws(aws);
        return this;
    }

    public EnvironmentNetworkTestDto withGcp(EnvironmentNetworkGcpParams gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public EnvironmentNetworkTestDto withYarn(EnvironmentNetworkYarnParams yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    @Deprecated
    public EnvironmentNetworkTestDto withNetworkCIDR(String networkCIDR) {
        getRequest().setAws(null);
        getRequest().setAzure(null);
        getRequest().setYarn(null);
        getRequest().setMock(null);
        getRequest().setSubnetIds(null);
        getRequest().setGcp(null);
        getRequest().setNetworkCidr(networkCIDR);
        return this;
    }

    public EnvironmentNetworkTestDto withPrivateSubnets() {
        getRequest().setPrivateSubnetCreation(PrivateSubnetCreation.ENABLED);
        return this;
    }

    public EnvironmentNetworkTestDto withServiceEndpoints() {
        getRequest().setServiceEndpointCreation(getCloudProvider().serviceEndpoint());
        return this;
    }

    public EnvironmentNetworkTestDto withServiceEndpoints(ServiceEndpointCreation serviceEndpointCreation) {
        getRequest().setServiceEndpointCreation(serviceEndpointCreation);
        return this;
    }

    public EnvironmentNetworkTestDto withNoOutboundInternetTraffic() {
        getRequest().setOutboundInternetTraffic(OutboundInternetTraffic.DISABLED);
        return this;
    }

    public EnvironmentNetworkTestDto withSubnetIDs(Set<String> subnetIDs) {
        getRequest().setSubnetIds(subnetIDs);
        return this;
    }

    public EnvironmentNetworkTestDto withMock(EnvironmentNetworkMockParams mock) {
        getRequest().setMock(mock);
        return this;
    }

    public EnvironmentNetworkTestDto withPublicEndpointAccessGateway(PublicEndpointAccessGateway publicEndpointAccessGateway) {
        getRequest().setPublicEndpointAccessGateway(publicEndpointAccessGateway);
        return this;
    }

    public EnvironmentNetworkTestDto withEndpointGatewaySubnetIds(Set<String> endpointGatewaySubnetIds) {
        getRequest().setEndpointGatewaySubnetIds(endpointGatewaySubnetIds);
        return this;
    }

}
