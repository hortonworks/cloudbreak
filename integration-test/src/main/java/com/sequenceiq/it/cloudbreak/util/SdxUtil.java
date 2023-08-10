package com.sequenceiq.it.cloudbreak.util;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Component
public class SdxUtil {

    public static final String MINIMUM_EDL_RUN_TIME_VERSION = "7.2.17";

    public List<String> getInstanceIds(AbstractSdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response().getInstanceGroups();
        return InstanceUtil.getInstanceIds(instanceGroups, hostGroupName);
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

    private SdxClusterDetailResponse getSdxClusterDetailResponse(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return sdxClient.getDefaultClient().sdxEndpoint().getDetail(testDto.getName(), new HashSet<>());
    }
}
