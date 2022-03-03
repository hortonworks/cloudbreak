package com.sequenceiq.it.cloudbreak.dto.sdx;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Prototype
public class SdxUpgradeTestDto extends AbstractSdxTestDto<SdxUpgradeRequest, SdxUpgradeResponse, SdxUpgradeTestDto> {
    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    public SdxUpgradeTestDto(SdxUpgradeRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxUpgradeTestDto(TestContext testContext) {
        super(new SdxUpgradeRequest(), testContext);
    }

    public SdxUpgradeTestDto() {
        super(SdxUpgradeTestDto.class.getSimpleName().toUpperCase());
    }

    @Override
    public SdxUpgradeTestDto valid() {
        return withRuntime(commonClusterManagerProperties.getUpgrade().getTargetRuntimeVersion())
                .withReplaceVms(SdxUpgradeReplaceVms.ENABLED)
                .setSkipBackup(Boolean.TRUE);

    }

    public SdxUpgradeTestDto withRuntime(String runtime) {
        getRequest().setRuntime(runtime);
        return this;
    }

    public SdxUpgradeTestDto withReplaceVms(SdxUpgradeReplaceVms replaceVms) {
        getRequest().setReplaceVms(replaceVms);
        return this;
    }

    public SdxUpgradeTestDto setSkipBackup(Boolean skipBackup) {
        getRequest().setSkipBackup(skipBackup);
        return this;
    }
}
