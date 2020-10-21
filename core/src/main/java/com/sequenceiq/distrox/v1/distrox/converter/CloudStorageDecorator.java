package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class CloudStorageDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageDecorator.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

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
                identity.setGcs(logging.getGcs());
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
        if (hasDatalake(datalakes)) {
            Pair<String, FileSystemType> sdxBaseLocationFileSystemType = getBaseLocationWithFileSystemTypeFromSdx(datalakes.get(0));
            Set<ConfigQueryEntry> recommendations = getRecommendations(blueprintName, clusterName, sdxBaseLocationFileSystemType);
            if (storageLocationsNotDefined(request)) {
                if (request == null) {
                    request = new CloudStorageRequest();
                }
                if (request.getLocations() == null) {
                    request.setLocations(new ArrayList<>());
                }
                for (ConfigQueryEntry recommendation : recommendations) {
                    request.getLocations().add(createStorageLocationBaseByTypeAndDefaultPath(recommendation.getType(), recommendation.getDefaultPath()));
                }
            } else {
                Map<CloudStorageCdpService, String> templatedLocations = findLocationsThatContainsTemplatedValue(request);
                if (!templatedLocations.isEmpty()) {
                    LOGGER.info("Cloud storage location(s) has found with template placeholder(s). About to replace them with the recommended one(s).");
                    Set<ConfigQueryEntry> filtered = filterConfigsWithTemplatePlaceholder(request, recommendations);
                    Set<ConfigQueryEntry> replaced = queryParameters(filtered, blueprintName, clusterName, sdxBaseLocationFileSystemType);
                    replaceTemplatedLocationValuesWithFilledValues(request, replaced, templatedLocations);
                }
            }
        }
        return request;
    }

    private Set<ConfigQueryEntry> filterConfigsWithTemplatePlaceholder(CloudStorageRequest request, Set<ConfigQueryEntry> recommendations) {
        Set<ConfigQueryEntry> filtered = new LinkedHashSet<>();
        for (StorageLocationBase location : request.getLocations()) {
            recommendations
                    .stream()
                    .filter(configQueryEntry -> configQueryEntry.getType().equals(location.getType()))
                    .findFirst()
                    .ifPresent(configQueryEntry -> {
                        ConfigQueryEntry custom = configQueryEntry.copy();
                        custom.setDefaultPath(location.getValue());
                        filtered.add(custom);
                    });
        }
        return filtered;
    }

    private Set<ConfigQueryEntry> queryParameters(Set<ConfigQueryEntry> filtered, String blueprintName, String clusterName,
            Pair<String, FileSystemType> sdxBaseLocationFileSystemType) {
        Pair<Blueprint, String> bt = blueprintService.getBlueprintAndText(blueprintName, 0L);
        FileSystemConfigQueryObject fsConfigO = blueprintService.createFileSystemConfigQueryObject(bt, clusterName,
                sdxBaseLocationFileSystemType.getLeft(), sdxBaseLocationFileSystemType.getRight().name(), "", true, false);
        return cmCloudStorageConfigProvider.queryParameters(filtered, fsConfigO);
    }

    private Set<ConfigQueryEntry> getRecommendations(String blueprintName, String clusterName, Pair<String, FileSystemType> sdxBaseLocationFileSystemType) {
        Set<ConfigQueryEntry> recommendations = new HashSet<>();
        if (!Strings.isNullOrEmpty(sdxBaseLocationFileSystemType.getLeft()) && sdxBaseLocationFileSystemType.getRight() != null) {
            LOGGER.debug("Getting file system recommendations for cluster: {}, blueprint: {}, base location: {}, file system type: {}",
                    clusterName, blueprintName, sdxBaseLocationFileSystemType.getLeft(), sdxBaseLocationFileSystemType.getRight().name());
            recommendations = getFileSystemRecommendations(blueprintName, clusterName, sdxBaseLocationFileSystemType.getLeft(),
                    sdxBaseLocationFileSystemType.getRight().name());
        }
        return recommendations;
    }

    private Pair<String, FileSystemType> getBaseLocationWithFileSystemTypeFromSdx(SdxClusterResponse sdxClusterResponse) {
        return Pair.of(sdxClusterResponse.getCloudStorageBaseLocation(), sdxClusterResponse.getCloudStorageFileSystemType());
    }

    private void replaceTemplatedLocationValuesWithFilledValues(CloudStorageRequest request, Set<ConfigQueryEntry> recommendations,
            Map<CloudStorageCdpService, String> templatedLocations) {
        recommendations
                .stream()
                .filter(entry -> templatedLocations.containsKey(entry.getType()))
                .forEach(cqe -> {
                    request.getLocations().removeIf(slb -> slb.getType().equals(cqe.getType()));
                    request.getLocations().add(createStorageLocationBaseByTypeAndDefaultPath(cqe.getType(), cqe.getDefaultPath()));
                });
    }

    private StorageLocationBase createStorageLocationBaseByTypeAndDefaultPath(CloudStorageCdpService type, String defaultPath) {
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(type);
        storageLocationBase.setValue(defaultPath);
        return storageLocationBase;
    }

    private Map<CloudStorageCdpService, String> findLocationsThatContainsTemplatedValue(CloudStorageRequest request) {
        return request.getLocations()
                .stream()
                .filter(slb -> slb.getValue().matches(".+\\/\\{{3}.+\\}{3}\\/?.*"))
                .collect(Collectors.toMap(slb -> slb.getType(), slb -> slb.getValue()));
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
