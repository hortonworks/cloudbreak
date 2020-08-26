package com.sequenceiq.freeipa.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class AvailabilityChecker {

    protected boolean isAvailable(Stack stack, Versioned supportedAfter) {
        if (StringUtils.isNotBlank(stack.getAppVersion())) {
            Versioned currentVersion = stack::getAppVersion;
            return new VersionComparator().compare(currentVersion, supportedAfter) > 0;
        } else {
            return false;
        }
    }

}
