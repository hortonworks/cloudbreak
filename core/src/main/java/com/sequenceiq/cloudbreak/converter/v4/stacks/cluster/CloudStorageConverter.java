package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.cloudstorage.WasbIdentity;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemResolver;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemAwareCloudStorage;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class CloudStorageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageConverter.class);

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Inject
    private FileSystemResolver fileSystemResolver;

    @Inject
    private CloudStorageParametersConverter cloudStorageParametersConverter;

    @Inject
    private ConversionService conversionService;

    public CloudStorageRequest fileSystemToRequest(FileSystem fileSystem) {
        CloudStorageRequest result = new CloudStorageRequest();
        CloudStorage cloudStorage = fileSystem.getCloudStorage();
        if (cloudStorage != null) {
            List<StorageIdentityBase> storageIdentityRequests = cloudStorage.getCloudIdentities().stream()
                    .map(this::cloudIdentityToRequest).collect(Collectors.toList());
            result.setIdentities(storageIdentityRequests);
            List<StorageLocationBase> storageLocationRequests = cloudStorage.getLocations().stream()
                    .map(this::storageLocationToRequest).collect(Collectors.toList());
            result.setLocations(storageLocationRequests);
            NullUtil.doIfNotNull(cloudStorage.getS3GuardDynamoTableName(), tableName -> {
                AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
                S3Guard s3Guard = new S3Guard();
                s3Guard.setDynamoTableName(tableName);
                awsStorageParameters.setS3Guard(s3Guard);
                result.setAws(awsStorageParameters);
            });
        }
        return result;
    }

    public FileSystem requestToFileSystem(CloudStorageBase cloudStorageRequest) {
        FileSystem fileSystem = new FileSystem();

        fileSystem.setName(nameGenerator.generateName(FILESYSTEM));
        FileSystemAwareCloudStorage cloudStorageParameters = fileSystemResolver.resolveFileSystem(cloudStorageRequest);
        fileSystem.setType(cloudStorageParameters.getType());

        CloudStorage cloudStorage = new CloudStorage();
        String s3GuardDynamoTableName = getS3GuardDynamoTableName(cloudStorageRequest);
        cloudStorage.setS3GuardDynamoTableName(s3GuardDynamoTableName);
        List<StorageLocation> storageLocations = cloudStorageRequest.getLocations().stream()
                .map(this::storageLocationRequestToStorageLocation).collect(Collectors.toList());
        cloudStorage.setLocations(storageLocations);

        List<CloudIdentity> cloudIdentities = cloudStorageRequest.getIdentities().stream()
                .map(this::identityRequestToCloudIdentity).collect(Collectors.toList());
        cloudStorage.setCloudIdentities(cloudIdentities);

        fileSystem.setCloudStorage(cloudStorage);

        return fileSystem;

    }

    private String getS3GuardDynamoTableName(CloudStorageBase cloudStorageRequest) {
        return cloudStorageRequest.getAws() != null
                ? cloudStorageRequest.getAws().getS3Guard() != null
                ? cloudStorageRequest.getAws().getS3Guard().getDynamoTableName() : null : null;
    }

    public SpiFileSystem requestToSpiFileSystem(CloudStorageBase cloudStorageRequest) {
        CloudFileSystemView cloudFileSystemView = null;
        FileSystemType type = null;
        if (!cloudStorageRequest.getIdentities().isEmpty()) {
            // TODO: add support for multiple cloudFileSystemViews or multiple SpiFileSystems
            StorageIdentityBase storageIdentity = cloudStorageRequest.getIdentities().get(0);
            if (storageIdentity != null) {
                if (storageIdentity.getAdls() != null) {
                    cloudFileSystemView = cloudStorageParametersConverter.adlsToCloudView(storageIdentity.getAdls());
                    type = FileSystemType.ADLS;
                } else if (storageIdentity.getGcs() != null) {
                    cloudFileSystemView = cloudStorageParametersConverter.gcsToCloudView(storageIdentity.getGcs());
                    type = FileSystemType.GCS;
                } else if (storageIdentity.getS3() != null) {
                    cloudFileSystemView = cloudStorageParametersConverter.s3ToCloudView(storageIdentity.getS3());
                    type = FileSystemType.S3;
                } else if (storageIdentity.getWasb() != null) {
                    cloudFileSystemView = cloudStorageParametersConverter.wasbToCloudView(storageIdentity.getWasb());
                    type = FileSystemType.WASB;
                } else if (storageIdentity.getAdlsGen2() != null) {
                    cloudFileSystemView = cloudStorageParametersConverter.adlsGen2ToCloudView(storageIdentity.getAdlsGen2());
                    type = FileSystemType.ADLS_GEN_2;
                }
            }
        }
        // TODO: add support for multiple cloudFileSystemViews or multiple SpiFileSystems
        return new SpiFileSystem("", type, cloudFileSystemView);
    }

    public CloudStorageResponse fileSystemToResponse(FileSystem fileSystem) {
        CloudStorageResponse response = new CloudStorageResponse();
        CloudStorage cloudStorage = fileSystem.getCloudStorage();
        if (cloudStorage != null) {
            if (StringUtils.isNotEmpty(cloudStorage.getS3GuardDynamoTableName())) {
                AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
                S3Guard s3Guard = new S3Guard();
                s3Guard.setDynamoTableName(cloudStorage.getS3GuardDynamoTableName());
                awsStorageParameters.setS3Guard(s3Guard);
                response.setAws(awsStorageParameters);
            }
            List<StorageIdentityBase> storageIdentities = cloudStorage.getCloudIdentities().stream()
                    .map(this::cloudIdentityToStorageIdentityBase).collect(Collectors.toList());
            response.setIdentities(storageIdentities);

            List<StorageLocationBase> storageLocations = cloudStorage.getLocations().stream().map(storageLocation -> {
                StorageLocationBase storageLocationBase = new StorageLocationBase();
                storageLocationBase.setType(storageLocation.getType());
                storageLocationBase.setValue(storageLocation.getValue());
                return storageLocationBase;
            }).collect(Collectors.toList());
            response.setLocations(storageLocations);
        }
        return response;
    }

    private StorageIdentityBase cloudIdentityToStorageIdentityBase(CloudIdentity cloudIdentity) {
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setType(cloudIdentity.getIdentityType());
        if (cloudIdentity.getWasbIdentity() != null) {
            WasbCloudStorageV1Parameters parameters = wasbIdentityToParameters(cloudIdentity.getWasbIdentity());
            storageIdentityBase.setWasb(parameters);
        } else if (cloudIdentity.getS3Identity() != null) {
            S3CloudStorageV1Parameters parameters = s3IdentityToParameters(cloudIdentity.getS3Identity());
            storageIdentityBase.setS3(parameters);
        }
        return storageIdentityBase;
    }

    private StorageIdentityBase cloudIdentityToRequest(CloudIdentity cloudIdentity) {
        StorageIdentityBase storageIdentityRequest = new StorageIdentityBase();
        storageIdentityRequest.setType(cloudIdentity.getIdentityType());
        if (cloudIdentity.getS3Identity() != null) {
            S3CloudStorageV1Parameters s3Parameters = s3IdentityToParameters(cloudIdentity.getS3Identity());
            storageIdentityRequest.setS3(s3Parameters);
        } else if (cloudIdentity.getWasbIdentity() != null) {
            WasbCloudStorageV1Parameters wasbParameters = wasbIdentityToParameters(cloudIdentity.getWasbIdentity());
            storageIdentityRequest.setWasb(wasbParameters);
        }
        return storageIdentityRequest;
    }

    private StorageLocationBase storageLocationToRequest(StorageLocation storageLocation) {
        StorageLocationBase storageLocationRequest = new StorageLocationBase();
        storageLocationRequest.setType(storageLocation.getType());
        storageLocationRequest.setValue(storageLocation.getValue());
        return storageLocationRequest;
    }

    private S3CloudStorageV1Parameters s3IdentityToParameters(S3Identity s3Identity) {
        S3CloudStorageV1Parameters s3Parameters = new S3CloudStorageV1Parameters();
        s3Parameters.setInstanceProfile(s3Identity.getInstanceProfile());
        return s3Parameters;
    }

    private WasbCloudStorageV1Parameters wasbIdentityToParameters(WasbIdentity wasbIdentity) {
        WasbCloudStorageV1Parameters wasbParameters = new WasbCloudStorageV1Parameters();
        wasbParameters.setAccountKey(wasbIdentity.getAccountKey());
        wasbParameters.setAccountName(wasbIdentity.getAccountName());
        wasbParameters.setSecure(wasbIdentity.isSecure());
        return wasbParameters;
    }

    private S3Identity s3ParametersToIdentity(S3CloudStorageV1Parameters s3CloudStorageV1Parameters) {
        S3Identity s3Identity = new S3Identity();
        s3Identity.setInstanceProfile(s3CloudStorageV1Parameters.getInstanceProfile());
        return s3Identity;
    }

    private WasbIdentity wasbParametersToIdentity(WasbCloudStorageV1Parameters wasbCloudStorageV1Parameters) {
        WasbIdentity wasbIdentity = new WasbIdentity();
        wasbIdentity.setAccountKey(wasbCloudStorageV1Parameters.getAccountKey());
        wasbIdentity.setAccountName(wasbCloudStorageV1Parameters.getAccountName());
        wasbIdentity.setSecure(wasbCloudStorageV1Parameters.isSecure());
        return wasbIdentity;
    }

    private StorageLocation storageLocationRequestToStorageLocation(StorageLocationBase storageLocationRequest) {
        StorageLocation storageLocation = new StorageLocation();
        try {
            storageLocation.setType(storageLocationRequest.getType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(String.format("StorageLocation type '%s' is not supported. Supported types: [%s].",
                    storageLocationRequest.getType(), CloudStorageCdpService.typeListing()), e);
        }
        storageLocation.setValue(storageLocationRequest.getValue());
        return storageLocation;
    }

    private CloudIdentity identityRequestToCloudIdentity(StorageIdentityBase storageIdentityRequest) {
        CloudIdentity cloudIdentity = new CloudIdentity();
        cloudIdentity.setIdentityType(storageIdentityRequest.getType());
        if (storageIdentityRequest.getS3() != null) {
            S3Identity s3Identity = identityRequestToS3(storageIdentityRequest);
            cloudIdentity.setS3Identity(s3Identity);
        }
        if (storageIdentityRequest.getWasb() != null) {
            WasbIdentity wasbIdentity = identityRequestToWasb(storageIdentityRequest);
            cloudIdentity.setWasbIdentity(wasbIdentity);
        }
        if (storageIdentityRequest.getAdlsGen2() != null) {
            throw new BadRequestException("ADLS Gen2 cloud storage is not (yet) supported.");
        }
        if (storageIdentityRequest.getAdls() != null) {
            throw new BadRequestException("ADLS cloud storage is not (yet) supported.");
        }
        if (storageIdentityRequest.getGcs() != null) {
            throw new BadRequestException("GCS cloud storage is not (yet) supported.");
        }
        return cloudIdentity;
    }

    private S3Identity identityRequestToS3(StorageIdentityBase storageIdentityRequest) {
        S3Identity s3Identity = new S3Identity();
        s3Identity.setInstanceProfile(storageIdentityRequest.getS3().getInstanceProfile());
        return s3Identity;
    }

    private WasbIdentity identityRequestToWasb(StorageIdentityBase storageIdentityRequest) {
        WasbIdentity wasbIdentity = new WasbIdentity();
        wasbIdentity.setAccountKey(storageIdentityRequest.getWasb().getAccountKey());
        wasbIdentity.setAccountName(storageIdentityRequest.getWasb().getAccountName());
        wasbIdentity.setSecure(storageIdentityRequest.getWasb().isSecure());
        return wasbIdentity;
    }
}
