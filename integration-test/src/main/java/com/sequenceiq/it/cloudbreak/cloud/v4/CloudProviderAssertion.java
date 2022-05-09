package com.sequenceiq.it.cloudbreak.cloud.v4;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public interface CloudProviderAssertion {

    CloudPlatform getCloudPlatform();

    void assertServiceEndpoint(EnvironmentTestDto environmentTestDto);

}