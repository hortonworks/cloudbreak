package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;

@Component
public class TargetMajorVersionToUpgradeTargetVersionConverter {

    public UpgradeTargetMajorVersion convert(TargetMajorVersion sourceTargetVersion) {
        switch (sourceTargetVersion) {
            case VERSION_11:
                return UpgradeTargetMajorVersion.VERSION_11;
            default:
                throw new RuntimeException("Unknown upgrade target version: %s" + sourceTargetVersion);
        }
    }
}
