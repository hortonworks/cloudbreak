package com.sequenceiq.it.cloudbreak.dto.envpublicapi;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Map;

import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatus;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;

@Prototype
public class EnvironmentPublicApiLastSyncStatusDto extends AbstractEnvironmentPublicApiTestDto<LastSyncStatusRequest, LastSyncStatusResponse,
        EnvironmentPublicApiLastSyncStatusDto> {
    public EnvironmentPublicApiLastSyncStatusDto(TestContext testContext) {
        super(new LastSyncStatusRequest(), testContext);
    }

    @Override
    public EnvironmentPublicApiLastSyncStatusDto valid() {
        getRequest().setEnvNameOrCrn(getEnvironmentCrn());
        return this;
    }

    public EnvironmentPublicApiLastSyncStatusDto withNameOrCrn(String nameOrCrn) {
        getRequest().setEnvNameOrCrn(nameOrCrn);
        return this;
    }

    public EnvironmentPublicApiLastSyncStatusDto when(Action<EnvironmentPublicApiLastSyncStatusDto, EnvironmentPublicApiClient> action) {
        return getTestContext().when(this, EnvironmentPublicApiClient.class, action, emptyRunningParameter());
    }

    public EnvironmentPublicApiLastSyncStatusDto await(SyncStatus syncStatus) {
        return getTestContext().await(this, Map.of("status", syncStatus), emptyRunningParameter());
    }
}
