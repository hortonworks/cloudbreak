package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaUserSyncStatusDto extends AbstractFreeIpaTestDto<String, SyncOperationStatus, FreeIpaUserSyncStatusDto> {

    public FreeIpaUserSyncStatusDto(TestContext testContext) {
        super(testContext.given(EnvironmentTestDto.class).getCrn(), testContext);
    }

    public FreeIpaUserSyncStatusDto withEnvironmentCrn(String environmentCrn) {
        setRequest(environmentCrn);
        return this;
    }

    public FreeIpaUserSyncStatusDto withEnvironmentCrn() {
        setRequest(getTestContext().given(EnvironmentTestDto.class).getCrn());
        return this;
    }

    @Override
    public FreeIpaUserSyncStatusDto valid() {
        setRequest(getTestContext().given(EnvironmentTestDto.class).getCrn());
        return this;
    }
}
