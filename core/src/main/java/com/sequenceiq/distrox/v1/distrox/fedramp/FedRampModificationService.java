package com.sequenceiq.distrox.v1.distrox.fedramp;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;

@Service
public class FedRampModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedRampModificationService.class);

    @Inject
    private CommonGovService commonGovService;

    @Inject
    private ProviderPreferencesService preferencesService;

    @Inject
    private EntitlementService entitlementService;

    public void prepare(DistroXV1Request request, String accountId) {
        boolean govCloudDeployment = commonGovService.govCloudDeployment(
                preferencesService.enabledGovPlatforms(),
                preferencesService.enabledPlatforms());
        if (govCloudDeployment) {
            LOGGER.info("The current deployment is a fedramp deployment so needs to modify the distrox request to use external database");
            if (!entitlementService.isFedRampExternalDatabaseForceDisabled(accountId)) {
                LOGGER.info("CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED NOT applied on the tenant so adding HA external database to the distrox request");
                if (request.getExternalDatabase() == null) {
                    DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
                    request.setExternalDatabase(distroXDatabaseRequest);
                }
                request.getExternalDatabase().setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
            } else {
                LOGGER.info("CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED applied on the tenant so we will not modify the distrox request.");
            }
        }
    }
}
