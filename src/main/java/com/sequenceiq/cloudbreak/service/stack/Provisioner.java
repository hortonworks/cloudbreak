package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface Provisioner {

    void buildStack(Stack stack, String userData, Map<String, Object> setupProperties);

    CloudPlatform getCloudPlatform();

}
