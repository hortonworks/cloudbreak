package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class CmVersionMatcher {

    public boolean isCmVersionMatching(Image current, Image candidate) {
        String currentCmBuildNumber = current.getCmBuildNumber();
        String candidateCmBuildNumber = candidate.getCmBuildNumber();
        return StringUtils.isNoneBlank(currentCmBuildNumber, candidateCmBuildNumber)
                && currentCmBuildNumber.equals(candidateCmBuildNumber);
    }
}
