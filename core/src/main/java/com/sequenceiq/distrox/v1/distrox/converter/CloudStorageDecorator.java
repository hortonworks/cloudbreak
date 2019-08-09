package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageDecorator {

    public CloudStorageRequest decorate(CloudStorageRequest request, DetailedEnvironmentResponse environment) {
        if (environment != null) {
            if (request == null) {
                request = new CloudStorageRequest();
            }
            TelemetryResponse telemetry = environment.getTelemetry();
            if (telemetry != null && telemetry.getLogging() != null) {
                LoggingResponse logging = telemetry.getLogging();
                StorageIdentityBase identity = new StorageIdentityBase();
                identity.setType(CloudIdentityType.LOG);
                identity.setS3(logging.getS3());
                identity.setWasb(logging.getWasb());
                List<StorageIdentityBase> identities = request.getIdentities();
                if (identities == null) {
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

            if (dynamoDBTableNameSpecified(environment)) {
                String dynamoDbTableName = environment.getAws().getS3guard().getDynamoDbTableName();
                S3Guard s3Guard = new S3Guard();
                s3Guard.setDynamoTableName(dynamoDbTableName);
                AwsStorageParameters aws = new AwsStorageParameters();
                aws.setS3Guard(s3Guard);
                request.setAws(aws);
            }
        }
        return request;
    }

    private boolean dynamoDBTableNameSpecified(@NotNull DetailedEnvironmentResponse environment) {
        return environment.getAws() != null
                && environment.getAws().getS3guard() != null
                && StringUtils.isNotEmpty(environment.getAws().getS3guard().getDynamoDbTableName());
    }
}
