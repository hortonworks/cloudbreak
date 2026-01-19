package com.sequenceiq.it.cloudbreak.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Component
public class DistroxUtil {
    public List<String> getInstanceIds(DistroXTestDto testDto, CloudbreakClient cloudbreakClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = cloudbreakClient.getDefaultClient(testDto.getTestContext()).distroXV1Endpoint()
                .getByName(testDto.getName(), new HashSet<>()).getInstanceGroups();
        return InstanceUtil.getInstanceIds(instanceGroups, hostGroupName);
    }

    public Map<String, String> getInstancesWithAz(DistroXTestDto testDto, CloudbreakClient cloudbreakClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = cloudbreakClient.getDefaultClient(testDto.getTestContext()).distroXV1Endpoint()
                .getByName(testDto.getName(), new HashSet<>()).getInstanceGroups();
        return InstanceUtil.getInstancesWithAz(instanceGroups, hostGroupName);
    }
}
