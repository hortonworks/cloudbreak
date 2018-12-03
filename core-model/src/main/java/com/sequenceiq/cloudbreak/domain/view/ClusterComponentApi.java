package com.sequenceiq.cloudbreak.domain.view;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.json.Json;

public interface ClusterComponentApi {

    ComponentType getComponentType();

    String getName();

    Json getAttributes();
}
