package com.sequenceiq.it.cloudbreak.dto.distrox.cluster;

import jakarta.inject.Inject;

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

    @Override
    public DistroXUpgradeTestDto valid() {
        return withReplaceVms(DistroXUpgradeReplaceVms.DISABLED);
    }

    public DistroXUpgradeTestDto withRuntime(String runtime) {
        getRequest().setRuntime(runtime);
        getRequest().setImageId(null);
        return this;
    }

    public DistroXUpgradeTestDto withReplaceVms(DistroXUpgradeReplaceVms replaceVms) {
        getRequest().setReplaceVms(replaceVms);
        return this;
    }

    public DistroXUpgradeTestDto withImageId(String imageId) {
        getRequest().setImageId(imageId);
        getRequest().setRuntime(null);
        return this;
    }

    public DistroXUpgradeTestDto withLockComponents(Boolean lockComponents) {
        getRequest().setLockComponents(lockComponents);
        return this;
    }

    public DistroXUpgradeTestDto withRollingUpgradeEnabled(Boolean rollingUpgradeEnabled) {
        getRequest().setRollingUpgradeEnabled(rollingUpgradeEnabled);
        return this;
    }
}
