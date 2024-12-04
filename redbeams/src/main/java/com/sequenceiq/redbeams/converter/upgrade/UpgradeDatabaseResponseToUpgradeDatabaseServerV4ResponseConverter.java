package com.sequenceiq.redbeams.converter.upgrade;

import org.springframework.stereotype.Component;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseResponse;

@Component
public class UpgradeDatabaseResponseToUpgradeDatabaseServerV4ResponseConverter {

    public UpgradeDatabaseServerV4Response convert(UpgradeDatabaseResponse upgradeDatabaseResponse) {
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setCurrentVersion(upgradeDatabaseResponse.getCurrentVersion());
        response.setReason(upgradeDatabaseResponse.getReason());
        response.setWarning(upgradeDatabaseResponse.isWarning());
        response.setFlowIdentifier(upgradeDatabaseResponse.getFlowIdentifier());
        return response;
    }
}
