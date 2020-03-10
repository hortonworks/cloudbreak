package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.common.api.type.InstanceGroupType;

public interface InstanceMetaDataGroupView {

    String getGroupName();

    InstanceGroupType getInstanceGroupType();
}
