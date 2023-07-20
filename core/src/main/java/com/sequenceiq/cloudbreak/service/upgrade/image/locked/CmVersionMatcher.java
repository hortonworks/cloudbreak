package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class CmVersionMatcher {

    public boolean isCmVersionMatching(String cmBuildNumber, Image candidate) {
        String candidateCmBuildNumber = candidate.getCmBuildNumber();
        return StringUtils.isNoneBlank(cmBuildNumber, candidateCmBuildNumber)
                && cmBuildNumber.equals(candidateCmBuildNumber);
    }
}
