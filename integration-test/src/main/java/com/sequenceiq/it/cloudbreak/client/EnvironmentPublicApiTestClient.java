package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.environment.publicapi.EnvironmentPublicApiDescribeAction;
import com.sequenceiq.it.cloudbreak.action.environment.publicapi.EnvironmentPublicApiGetLastSyncStatusAction;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiLastSyncStatusDto;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;

@Service
public class EnvironmentPublicApiTestClient {
    public Action<EnvironmentPublicApiTestDto, EnvironmentPublicApiClient> describe() {
        return new EnvironmentPublicApiDescribeAction();
    }

    public Action<EnvironmentPublicApiLastSyncStatusDto, EnvironmentPublicApiClient> getLastSyncStatus() {
        return new EnvironmentPublicApiGetLastSyncStatusAction();
    }
}
