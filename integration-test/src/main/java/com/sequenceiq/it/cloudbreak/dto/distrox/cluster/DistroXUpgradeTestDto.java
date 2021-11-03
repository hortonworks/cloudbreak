package com.sequenceiq.it.cloudbreak.dto.distrox.cluster;

import javax.inject.Inject;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;

@Prototype
public class DistroXUpgradeTestDto extends AbstractSdxTestDto<DistroXUpgradeV1Request, DistroXUpgradeV1Response, DistroXUpgradeTestDto> {

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    public DistroXUpgradeTestDto(DistroXUpgradeV1Request request, TestContext testContext) {
        super(request, testContext);
    }

    public DistroXUpgradeTestDto(TestContext testContext) {
        super(new DistroXUpgradeV1Request(), testContext);
    }

    public DistroXUpgradeTestDto() {
        super(DistroXUpgradeTestDto.class.getSimpleName().toUpperCase());
    }

    @Override
    public DistroXUpgradeTestDto valid() {
        return withRuntime(commonClusterManagerProperties.getUpgrade().getTargetRuntimeVersion())
                .withReplaceVms(DistroXUpgradeReplaceVms.DISABLED);
    }

    public DistroXUpgradeTestDto withRuntime(String runtime) {
        getRequest().setRuntime(runtime);
        return this;
    }

    public DistroXUpgradeTestDto withReplaceVms(DistroXUpgradeReplaceVms replaceVms) {
        getRequest().setReplaceVms(replaceVms);
        return this;
    }

    public DistroXUpgradeTestDto withLockComponents(Boolean lockComponents) {
        getRequest().setLockComponents(lockComponents);
        return this;
    }
}
