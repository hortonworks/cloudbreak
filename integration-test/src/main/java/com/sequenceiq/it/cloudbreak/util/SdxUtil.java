package com.sequenceiq.it.cloudbreak.util;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;

@Component
public class SdxUtil {

    public List<String> getInstanceIds(AbstractSdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = sdxClient.getSdxClient().sdxEndpoint().getDetail(testDto.getName(), new HashSet<>())
                .getStackV4Response().getInstanceGroups();
        return InstanceUtil.getInstanceIds(instanceGroups, hostGroupName);
    }
}
