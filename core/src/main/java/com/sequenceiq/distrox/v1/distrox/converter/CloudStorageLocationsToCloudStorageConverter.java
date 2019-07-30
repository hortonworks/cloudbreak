package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageLocationsToCloudStorageConverter {

    public CloudStorageRequest convert(Set<StorageLocationBase> cloudStorageLocations, @NotNull DetailedEnvironmentResponse environment) {
        TelemetryResponse telemetry = environment.getTelemetry();
        CloudStorageRequest storageRequest = new CloudStorageRequest();
        boolean fieldUpdated = false;
        if (telemetry != null && telemetry.getLogging() != null) {
            LoggingResponse logging = telemetry.getLogging();
            StorageIdentityBase identity = new StorageIdentityBase();
            identity.setType(CloudIdentityType.LOG);
            identity.setS3(logging.getS3());
            identity.setWasb(logging.getWasb());
            storageRequest.setIdentities(Collections.singletonList(identity));
            fieldUpdated = true;
        }

        if (CollectionUtils.isNotEmpty(cloudStorageLocations)) {
            storageRequest.setLocations(new ArrayList<>(cloudStorageLocations));
            fieldUpdated = true;
        }

        if (dynamoDBTableNameSpecified(environment)) {
            String dynamoDbTableName = environment.getAws().getS3guard().getDynamoDbTableName();
            S3Guard s3Guard = new S3Guard();
            s3Guard.setDynamoTableName(dynamoDbTableName);
            AwsStorageParameters aws = new AwsStorageParameters();
            aws.setS3Guard(s3Guard);
            storageRequest.setAws(aws);
            fieldUpdated = true;
        }
        return fieldUpdated ? storageRequest : null;
    }

    private boolean dynamoDBTableNameSpecified(@NotNull DetailedEnvironmentResponse environment) {
        return environment.getAws() != null
                && environment.getAws().getS3guard() != null
                && StringUtils.isNotEmpty(environment.getAws().getS3guard().getDynamoDbTableName());
    }
}
