package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;

@Component
public class TargetMajorVersionToUpgradeTargetVersionConverter {

    public UpgradeTargetMajorVersion convert(TargetMajorVersion sourceTargetVersion) {
        return switch (sourceTargetVersion) {
            case VERSION_11 -> UpgradeTargetMajorVersion.VERSION_11;
            case VERSION_14 -> UpgradeTargetMajorVersion.VERSION_14;
            case VERSION11 -> UpgradeTargetMajorVersion.VERSION_11;
            case VERSION14 -> UpgradeTargetMajorVersion.VERSION_14;
            case VERSION17 -> UpgradeTargetMajorVersion.VERSION_17;
        };
    }
}
