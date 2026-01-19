package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

@Component
public class FreeIpaInstanceUtil {

    public List<String> getInstanceIds(FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient, String hostGroupName) {
        DescribeFreeIpaResponse describeFreeIpaResponse = freeIpaClient.getDefaultClient(freeIpaTestDto.getTestContext())
                .getFreeIpaV1Endpoint().describe(freeIpaTestDto.getRequest().getEnvironmentCrn());
        List<InstanceGroupResponse> instanceGroupResponses = describeFreeIpaResponse.getInstanceGroups();
        InstanceGroupResponse instanceGroupResponse = instanceGroupResponses.stream().filter(instanceGroup ->
                instanceGroup.getName().contains(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupResponse).getMetaData()
                .stream().map(InstanceMetaDataResponse::getInstanceId).collect(Collectors.toList());
    }

    public Map<List<String>, InstanceStatus> getInstanceStatusMap(DescribeFreeIpaResponse freeIpaResponse) {
        return freeIpaResponse.getInstanceGroups().stream()
                .filter(instanceGroupResponse -> instanceGroupResponse.getMetaData().stream()
                        .anyMatch(instanceMetaDataResponse -> Objects.nonNull(instanceMetaDataResponse.getInstanceId())))
                .collect(Collectors.toMap(
                        instanceGroupResponse -> instanceGroupResponse.getMetaData().stream()
                                .map(InstanceMetaDataResponse::getInstanceId)
                                .filter(Objects::nonNull).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> InstanceStatus.CREATED));
    }

}
