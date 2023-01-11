package com.sequenceiq.distrox.v1.distrox.service.upgrade.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public final class DistroXUpgradeDto {

    private final UpgradeV4Response upgradeV4Response;

    private final ImageChangeDto imageChangeDto;

    private final ImageInfoV4Response targetImage;

    private final boolean lockComponents;

    private final StackDto stackDto;

    public DistroXUpgradeDto(UpgradeV4Response upgradeV4Response, ImageChangeDto imageChangeDto, ImageInfoV4Response targetImage, boolean lockComponents,
            StackDto stackDto) {
        this.upgradeV4Response = upgradeV4Response;
        this.imageChangeDto = imageChangeDto;
        this.targetImage = targetImage;
        this.lockComponents = lockComponents;
        this.stackDto = stackDto;
    }

    public UpgradeV4Response getUpgradeV4Response() {
        return upgradeV4Response;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public ImageInfoV4Response getTargetImage() {
        return targetImage;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public StackDto getStackDto() {
        return stackDto;
    }

}
