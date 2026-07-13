package com.sequenceiq.freeipa.service.upgrade;

import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

public record FreeIpaUpgradeValidationResult(Stack stack, Set<InstanceMetaData> allInstances, ImageInfoResponse currentImage, ImageInfoResponse selectedImage) {
}
