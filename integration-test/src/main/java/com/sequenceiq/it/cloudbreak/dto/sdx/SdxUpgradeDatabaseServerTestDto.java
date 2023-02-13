package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;

@Prototype
public class SdxUpgradeDatabaseServerTestDto extends AbstractSdxTestDto<SdxUpgradeDatabaseServerRequest, SdxUpgradeDatabaseServerResponse,
        SdxUpgradeDatabaseServerTestDto> {
    public SdxUpgradeDatabaseServerTestDto(SdxUpgradeDatabaseServerRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxUpgradeDatabaseServerTestDto(TestContext testContext) {
        super(new SdxUpgradeDatabaseServerRequest(), testContext);
    }

    @Override
    public SdxUpgradeDatabaseServerTestDto valid() {
        return this;
    }

    public SdxUpgradeDatabaseServerTestDto withTargetMajorVersion(TargetMajorVersion targetMajorVersion) {
        getRequest().setTargetMajorVersion(targetMajorVersion);
        return this;
    }
}
