package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.cloudbreak.common.gov.CommonGovService.GOV;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class FreeipaPlatformStringTransformer {

    public String getPlatformString(Stack stack) {
        String platformVariant = stack.getPlatformvariant();
        String platform = stack.getCloudPlatform().toLowerCase();
        if (Strings.isNullOrEmpty(platformVariant)) {
            return platform;
        } else if (platformVariant.toLowerCase().endsWith(GOV)) {
            return platform.concat(GOV).toLowerCase();
        } else {
            return platform;
        }
    }
}
