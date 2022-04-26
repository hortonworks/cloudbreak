package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ScalabilityOption;

public interface InstanceGroupDto {

    String getGroupName();

    InstanceGroupType getInstanceGroupType();

    Long getId();

    Template getTemplate();

    SecurityGroup getSecurityGroup();

    Json getAttributes();

    int getMinimumNodeCount();

    InstanceGroupNetwork getInstanceGroupNetwork();

    ScalabilityOption getScalabilityOption();

    Set<String> getAvailabilityZones();
}
