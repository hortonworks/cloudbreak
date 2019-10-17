package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class CloudStorageDecorator {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private SdxClientService sdxClientService;

    public CloudStorageRequest decorate(String blueprintName,
        String clusterName,
        CloudStorageRequest request,
        DetailedEnvironmentResponse environment) {
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
                identity.setAdlsGen2(logging.getAdlsGen2());
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
                request.setIdentities(identities);
            }
            List<SdxClusterResponse> datalakes = sdxClientService.getByEnvironmentCrn(environment.getCrn());
            updateCloudStorageLocations(blueprintName, clusterName, request, datalakes);
            updateDynamoDBTable(request, environment);
        }
        return request;
    }

    public void updateDynamoDBTable(CloudStorageRequest request, DetailedEnvironmentResponse environment) {
        if (dynamoDBTableNameSpecified(environment)) {
            String dynamoDbTableName = environment.getAws().getS3guard().getDynamoDbTableName();
            S3Guard s3Guard = new S3Guard();
            s3Guard.setDynamoTableName(dynamoDbTableName);
            AwsStorageParameters aws = new AwsStorageParameters();
            aws.setS3Guard(s3Guard);
            request.setAws(aws);
        }
    }

    public CloudStorageRequest updateCloudStorageLocations(String blueprintName, String clusterName,
        CloudStorageRequest request, List<SdxClusterResponse> datalakes) {
        if (hasDatalake(datalakes) && storageLocationsNotDefined(request)) {
            if (request == null) {
                request = new CloudStorageRequest();
            }
            SdxClusterResponse sdxClusterResponse = datalakes.get(0);
            String baseLocation = sdxClusterResponse.getCloudStorageBaseLocation();
            FileSystemType fileSystemType = sdxClusterResponse.getCloudStorageFileSystemType();
            Set<ConfigQueryEntry> recommendations = new HashSet<>();
            if (!Strings.isNullOrEmpty(baseLocation) && fileSystemType != null) {
                recommendations = getFileSystemRecommendations(blueprintName, clusterName, baseLocation, fileSystemType.name());
            }
            if (request.getLocations() == null) {
                request.setLocations(new ArrayList<>());
            }
            for (ConfigQueryEntry recommendation : recommendations) {
                StorageLocationBase storageLocationBase = new StorageLocationBase();
                storageLocationBase.setType(recommendation.getType());
                storageLocationBase.setValue(recommendation.getDefaultPath());
                request.getLocations().add(storageLocationBase);
            }
        }
        return request;
    }

    private boolean hasDatalake(List<SdxClusterResponse> clusterResponses) {
        return clusterResponses != null && !clusterResponses.isEmpty();
    }

    public boolean storageLocationsNotDefined(CloudStorageRequest request) {
        return request == null || request.getLocations() == null || request.getLocations().isEmpty();
    }

    private Set<ConfigQueryEntry> getFileSystemRecommendations(
        String blueprint,
        String clusterName,
        String storageName,
        String type) {
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(
                blueprint,
                clusterName,
                storageName,
                type,
                "",
                true,
                false,
                0L);
        return entries;
    }

    private boolean dynamoDBTableNameSpecified(@NotNull DetailedEnvironmentResponse environment) {
        return environment.getAws() != null
                && environment.getAws().getS3guard() != null
                && StringUtils.isNotEmpty(environment.getAws().getS3guard().getDynamoDbTableName());
    }
}
