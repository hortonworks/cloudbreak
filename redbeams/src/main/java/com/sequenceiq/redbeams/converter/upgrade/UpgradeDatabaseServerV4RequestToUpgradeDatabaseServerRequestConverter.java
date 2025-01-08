package com.sequenceiq.redbeams.converter.upgrade;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.converter.stack.DatabaseServerV4StackRequestToDatabaseServerConverter;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;

@Component
public class UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter {

    @Inject
    private DatabaseServerV4StackRequestToDatabaseServerConverter databaseServerV4StackRequestToDatabaseServerConverter;

    public UpgradeDatabaseRequest convert(UpgradeDatabaseServerV4Request upgradeDatabaseServerV4Request) {
        DatabaseServer migratedDatabaseServer = NullUtil.getIfNotNull(upgradeDatabaseServerV4Request.getUpgradedDatabaseSettings(),
                request -> databaseServerV4StackRequestToDatabaseServerConverter.buildDatabaseServer(request, request.getCloudPlatform()));
        UpgradeDatabaseRequest upgradeDatabaseRequest = new UpgradeDatabaseRequest();
        upgradeDatabaseRequest.setTargetMajorVersion(convert(upgradeDatabaseServerV4Request.getUpgradeTargetMajorVersion()));
        upgradeDatabaseRequest.setMigratedDatabaseServer(migratedDatabaseServer);
        return upgradeDatabaseRequest;
    }

    private TargetMajorVersion convert(UpgradeTargetMajorVersion sourceTargetVersion) {
        return switch (sourceTargetVersion) {
            case VERSION_11 -> TargetMajorVersion.VERSION_11;
            case VERSION_14 -> TargetMajorVersion.VERSION_14;
        };
    }

}