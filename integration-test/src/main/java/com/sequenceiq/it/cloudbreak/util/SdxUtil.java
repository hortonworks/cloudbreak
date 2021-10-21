package com.sequenceiq.it.cloudbreak.util;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Component
public class SdxUtil {

    public List<String> getInstanceIds(AbstractSdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response().getInstanceGroups();
        return InstanceUtil.getInstanceIds(instanceGroups, hostGroupName);
    }

    public String getShape(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getClusterShape().name();
    }

    public String getImageId(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return getSdxClusterDetailResponse(testDto, sdxClient)
                .getStackV4Response().getImage().getId();
    }

    private SdxClusterDetailResponse getSdxClusterDetailResponse(AbstractSdxTestDto testDto, SdxClient sdxClient) {
        return sdxClient.getDefaultClient().sdxEndpoint().getDetail(testDto.getName(), new HashSet<>());
    }
}
