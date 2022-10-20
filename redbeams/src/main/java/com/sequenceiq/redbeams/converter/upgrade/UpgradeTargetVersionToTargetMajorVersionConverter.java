package com.sequenceiq.redbeams.converter.upgrade;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;

@Component
public class UpgradeTargetVersionToTargetMajorVersionConverter {

    public TargetMajorVersion convert(UpgradeTargetMajorVersion sourceTargetVersion) {
        switch (sourceTargetVersion) {
            case VERSION_11:
                return TargetMajorVersion.VERSION_11;
            default:
                throw new RuntimeException("Unknown target version: %s" + sourceTargetVersion);
        }
    }
}
