package com.sequenceiq.it.cloudbreak.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Component
public class SdxUtil {

    public List<String> getInstanceIds(AbstractSdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response().getInstanceGroups();
        return InstanceUtil.getInstanceIds(instanceGroups, hostGroupName);
    }

    public Map<String, String> getInstancesWithAz(AbstractSdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response().getInstanceGroups();
        return InstanceUtil.getInstancesWithAz(instanceGroups, hostGroupName);
    }

    public String getShape(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getClusterShape().name();
    }

    public String getCrn(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getCrn();
    }

    public String getImageId(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response().getImage().getId();
    }

    public String getRuntime(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getRuntime();
    }

    public String getCloudStorageBaseLocation(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getCloudStorageBaseLocation();
    }

    public Long getCreated(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getCreated();
    }

    public String getStatusReason(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getStatusReason();
    }

    public List<LoadBalancerResponse> getLoadbalancers(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response()
                .getLoadBalancers();
    }

    private SdxClusterDetailResponse getSdxClusterDetailResponse(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return sdxClient.getDefaultClient(testDto.getTestContext()).sdxEndpoint().getDetail(testDto.getName(), new HashSet<>());
    }
}
