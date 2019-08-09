package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageLocationsToCloudStorageConverter {

    public CloudStorageRequest convert(CloudStorageRequest request, DetailedEnvironmentResponse environment) {
        TelemetryResponse telemetry = environment.getTelemetry();
        if (telemetry != null && telemetry.getLogging() != null) {
            if (request == null) {
                request = new CloudStorageRequest();
            }
            LoggingResponse logging = telemetry.getLogging();
            StorageIdentityBase identity = new StorageIdentityBase();
            identity.setType(CloudIdentityType.LOG);
            identity.setS3(logging.getS3());
            identity.setWasb(logging.getWasb());
            List<StorageIdentityBase> identities = request.getIdentities();
            if (identities == null) { // should not be needed, but handles that, when identities is set as null
                identities = new ArrayList<>();
            }
            boolean logConfiguredInRequest = false;
            for (StorageIdentityBase identityBase : identities) {
                if (CloudIdentityType.LOG.equals(identityBase.getType())) {
                    logConfiguredInRequest = true;
                }
            }
            if (!logConfiguredInRequest) {
                identities.add(identity);
            }
        }
        return request;
    }
}
