package com.sequenceiq.it.cloudbreak.util;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

@Component
public class DistroxUtil {
    public List<String> getInstanceIds(DistroXTestDto testDto, CloudbreakClient cloudbreakClient, String hostGroupName) {
        List<InstanceGroupV4Response> instanceGroups = cloudbreakClient.getCloudbreakClient().distroXV1Endpoint()
                .getByName(testDto.getName(), new HashSet<>()).getInstanceGroups();
        return InstanceUtil.getInstanceIds(instanceGroups, hostGroupName);
    }
}
