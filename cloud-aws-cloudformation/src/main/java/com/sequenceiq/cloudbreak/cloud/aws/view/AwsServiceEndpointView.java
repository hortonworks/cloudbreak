package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

public class AwsServiceEndpointView {
    private final String serviceName;

    private final String serviceEndpointTemplateName;

    private final List<SubnetRequest> subnetRequests;

    public AwsServiceEndpointView(String serviceName, List<SubnetRequest> subnetRequests) {
        this.serviceName = serviceName;
        this.serviceEndpointTemplateName = createServiceEndpointTemplateName(serviceName);
        this.subnetRequests = subnetRequests;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceEndpointTemplateName() {
        return serviceEndpointTemplateName;
    }

    public List<SubnetRequest> getSubnetRequests() {
        return subnetRequests;
    }

    @Override
    public String toString() {
        return "AwsServiceEndpointView{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceEndpointTemplateName='" + serviceEndpointTemplateName + '\'' +
                ", subnetRequests=" + subnetRequests +
                '}';
    }

    private String createServiceEndpointTemplateName(String serviceName) {
        return serviceName.replaceAll("[-\\.]", "") + "Endpoint";
    }
}
