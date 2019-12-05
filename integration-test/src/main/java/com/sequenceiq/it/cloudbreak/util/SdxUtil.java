package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Component
public class SdxUtil {

    public List<String> getInstanceIds(SdxTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        Set<String> entries = new HashSet<>();
        List<String> instanceIds = new ArrayList<>();

        InstanceGroupV4Response instanceGroupV4Response = sdxClient.getSdxClient().sdxEndpoint().getDetail(testDto.getName(), entries)
                .getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                .findFirst()
                .orElse(null);
        InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().findFirst().orElse(null);
        instanceIds.add(Objects.requireNonNull(instanceMetaDataV4Response).getInstanceId());
        return instanceIds;
    }

    public List<String> getInstanceIds(SdxInternalTestDto testDto, SdxClient sdxClient, String hostGroupName) {
        Set<String> entries = new HashSet<>();
        List<String> instanceIds = new ArrayList<>();

        InstanceGroupV4Response instanceGroupV4Response = sdxClient.getSdxClient().sdxEndpoint().getDetail(testDto.getName(), entries)
                .getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                .findFirst()
                .orElse(null);
        InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().findFirst().orElse(null);
        instanceIds.add(Objects.requireNonNull(instanceMetaDataV4Response).getInstanceId());
        return instanceIds;
    }
}
