package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ApiModel(value = "EnvironmentLoadBalancerUpdateResponse")
public class EnvironmentLoadBalancerUpdateResponse {

    @ApiModelProperty(EnvironmentModelDescription.PUBLIC_ENDPOINT_ACCESS_GATEWAY)
    private PublicEndpointAccessGateway requestedPublicEndpointGateway = PublicEndpointAccessGateway.DISABLED;

    @ApiModelProperty(EnvironmentModelDescription.ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS)
    private Set<String> requestedEndpointSubnetIds = Set.of();

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_STATUS)
    private LoadBalancerUpdateStatus status = LoadBalancerUpdateStatus.NOT_STARTED;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_FLOWID)
    private FlowIdentifier flowId;

    public PublicEndpointAccessGateway getRequestedPublicEndpointGateway() {
        return requestedPublicEndpointGateway;
    }

    public void setRequestedPublicEndpointGateway(PublicEndpointAccessGateway requestedPublicEndpointGateway) {
        this.requestedPublicEndpointGateway = requestedPublicEndpointGateway;
    }

    public Set<String> getRequestedEndpointSubnetIds() {
        return requestedEndpointSubnetIds;
    }

    public void setRequestedEndpointSubnetIds(Set<String> requestedEndpointSubnetIds) {
        this.requestedEndpointSubnetIds = requestedEndpointSubnetIds;
    }

    public LoadBalancerUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerUpdateStatus status) {
        this.status = status;
    }

    public FlowIdentifier getFlowId() {
        return flowId;
    }

    public void setFlowId(FlowIdentifier flowId) {
        this.flowId = flowId;
    }

    @Override
    public String toString() {
        return "EnvironmentLoadBalancerUpdateResponse{" +
            "requestedPublicEndpointGateway=" + requestedPublicEndpointGateway +
            ", requestedEndpointSubnetIds=" + requestedEndpointSubnetIds +
            ", status=" + status +
            ", flowId=" + flowId +
            '}';
    }
}
