package com.sequenceiq.redbeams.converter.upgrade;

import org.springframework.stereotype.Component;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;

@Component
public class UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter {

    public UpgradeDatabaseRequest convert(UpgradeDatabaseServerV4Request upgradeDatabaseServerV4Request) {
        UpgradeDatabaseRequest upgradeDatabaseRequest = new UpgradeDatabaseRequest();
        upgradeDatabaseRequest.setTargetMajorVersion(convert(upgradeDatabaseServerV4Request.getUpgradeTargetMajorVersion()));
        return upgradeDatabaseRequest;
    }

    private TargetMajorVersion convert(UpgradeTargetMajorVersion sourceTargetVersion) {
        switch (sourceTargetVersion) {
            case VERSION_11:
                return TargetMajorVersion.VERSION_11;
            default:
                throw new RuntimeException("Unknown target version: %s" + sourceTargetVersion);
        }
    }

}
