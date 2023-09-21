package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.cloudbreak.common.gov.CommonGovService.GOV;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class FreeipaPlatformStringTransformer {

    public String getPlatformString(Stack stack) {
        String platformVariant = stack.getPlatformvariant();
        String platform = stack.getCloudPlatform().toLowerCase(Locale.ROOT);
        if (Strings.isNullOrEmpty(platformVariant)) {
            return platform;
        } else if (platformVariant.toLowerCase(Locale.ROOT).endsWith(GOV)) {
            return platform.concat(GOV).toLowerCase(Locale.ROOT);
        } else {
            return platform;
        }
    }
}
