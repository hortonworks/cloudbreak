package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AdlsGen2Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.cloudstorage.WasbIdentity;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemResolver;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

@Component
public class CloudStorageConverter {

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Inject
    private FileSystemResolver fileSystemResolver;

    @Inject
    private CloudStorageParametersConverter cloudStorageParametersConverter;

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

            result.setAccountMapping(accountMappingToAccountMappingRequest(cloudStorage.getAccountMapping()));
        }
        return result;
    }

    public FileSystem requestToFileSystem(CloudStorageBase cloudStorageRequest) {
        FileSystem fileSystem = new FileSystem();

        fileSystem.setName(nameGenerator.generateName(FILESYSTEM));
        FileSystemType fileSystemType = fileSystemResolver.determineFileSystemType(cloudStorageRequest);
        fileSystem.setType(fileSystemType);

        CloudStorage cloudStorage = new CloudStorage();
        String s3GuardDynamoTableName = getS3GuardDynamoTableName(cloudStorageRequest);
        cloudStorage.setS3GuardDynamoTableName(s3GuardDynamoTableName);
        List<StorageLocation> storageLocations = cloudStorageRequest.getLocations().stream()
                .map(this::storageLocationRequestToStorageLocation).collect(Collectors.toList());
        cloudStorage.setLocations(storageLocations);

        if (cloudStorageRequest.getIdentities() != null) {

            List<CloudIdentity> cloudIdentities = cloudStorageRequest.getIdentities().stream()
                    .map(this::identityRequestToCloudIdentity).collect(Collectors.toList());
            cloudStorage.setCloudIdentities(cloudIdentities);
        }

        cloudStorage.setAccountMapping(accountMappingRequestToAccountMapping(cloudStorageRequest.getAccountMapping()));
        fileSystem.setCloudStorage(cloudStorage);

        return fileSystem;

    }

    private String getS3GuardDynamoTableName(CloudStorageBase cloudStorageRequest) {
        return cloudStorageRequest.getAws() != null
                ? cloudStorageRequest.getAws().getS3Guard() != null
                ? cloudStorageRequest.getAws().getS3Guard().getDynamoTableName() : null : null;
    }

    public SpiFileSystem requestToSpiFileSystem(CloudStorageBase cloudStorageRequest) {
        List<CloudFileSystemView> cloudFileSystemViews = new ArrayList<>();
        FileSystemType type = null;
        if (cloudStorageRequest.getIdentities() != null && !cloudStorageRequest.getIdentities().isEmpty()) {
            for (StorageIdentityBase storageIdentity : cloudStorageRequest.getIdentities()) {
                type = getCloudFileSystemView(cloudStorageRequest, cloudFileSystemViews, storageIdentity);
            }
        }
        validateCloudFileSystemViews(cloudFileSystemViews, type);
        return new SpiFileSystem("", type, cloudFileSystemViews);
    }

    private FileSystemType getCloudFileSystemView(CloudStorageBase cloudStorageRequest,
                                                    List<CloudFileSystemView> cloudFileSystemViews,
                                                    StorageIdentityBase storageIdentity) {
        FileSystemType type = null;
        if (storageIdentity != null) {
            CloudFileSystemView cloudFileSystemView = null;
            if (storageIdentity.getAdls() != null) {
                cloudFileSystemView = cloudStorageParametersConverter.adlsToCloudView(storageIdentity);
                type = FileSystemType.ADLS;
            } else if (storageIdentity.getGcs() != null) {
                cloudFileSystemView = cloudStorageParametersConverter.gcsToCloudView(storageIdentity);
                type = FileSystemType.GCS;
            } else if (storageIdentity.getS3() != null) {
                cloudFileSystemView = cloudStorageParametersConverter.s3ToCloudView(storageIdentity);
                setDynamoDBTableName((CloudS3View) cloudFileSystemView, cloudStorageRequest);
                type = FileSystemType.S3;
            } else if (storageIdentity.getWasb() != null) {
                cloudFileSystemView = cloudStorageParametersConverter.wasbToCloudView(storageIdentity);
                type = FileSystemType.WASB;
            } else if (storageIdentity.getAdlsGen2() != null) {
                cloudFileSystemView = cloudStorageParametersConverter.adlsGen2ToCloudView(storageIdentity);
                type = FileSystemType.ADLS_GEN_2;
            }
            if (cloudFileSystemView != null) {
                cloudFileSystemView.setAccountMapping(cloudStorageRequest.getAccountMapping());
                cloudFileSystemView.setLocations(cloudStorageRequest.getLocations());
                cloudFileSystemViews.add(cloudFileSystemView);
            }
        }
        return type;
    }

    private void setDynamoDBTableName(CloudS3View cloudFileSystemView, CloudStorageBase cloudStorageRequest) {
        if (cloudStorageRequest.getAws() != null &&
                cloudStorageRequest.getAws().getS3Guard() != null &&
                cloudStorageRequest.getAws().getS3Guard().getDynamoTableName() != null) {
            String dynamoTableName = cloudStorageRequest.getAws().getS3Guard().getDynamoTableName();
            cloudFileSystemView.setS3GuardDynamoTableName(dynamoTableName);
        }
    }

    private void validateCloudFileSystemViews(List<CloudFileSystemView> cloudFileSystemViews, FileSystemType type) {
        if (type == FileSystemType.WASB_INTEGRATED) {
            throw new BadRequestException(type + " FileSystemType is not supported.");
        }
        if (cloudFileSystemViews.size() > 1) {
            if (type != FileSystemType.S3 && type != FileSystemType.ADLS_GEN_2) {
                throw new BadRequestException("Multiple identities for " + type + " is not supported yet.");
            }
        }
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

            response.setAccountMapping(accountMappingToAccountMappingRequest(cloudStorage.getAccountMapping()));
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
        } else if (cloudIdentity.getAdlsGen2Identity() != null) {
            AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = adlsGen2IdentityToParameters(cloudIdentity.getAdlsGen2Identity());
            storageIdentityBase.setAdlsGen2(adlsGen2CloudStorageV1Parameters);
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
        } else if (cloudIdentity.getAdlsGen2Identity() != null) {
            AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = adlsGen2IdentityToParameters(cloudIdentity.getAdlsGen2Identity());
            storageIdentityRequest.setAdlsGen2(adlsGen2CloudStorageV1Parameters);
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

    private AdlsGen2CloudStorageV1Parameters adlsGen2IdentityToParameters(AdlsGen2Identity adlsGen2Identity) {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setManagedIdentity(adlsGen2Identity.getManagedIdentity());
        return adlsGen2CloudStorageV1Parameters;
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
            AdlsGen2Identity identity = identityRequestToAdlsGen2(storageIdentityRequest);
            cloudIdentity.setAdlsGen2Identity(identity);
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

    private AdlsGen2Identity identityRequestToAdlsGen2(StorageIdentityBase storageIdentityRequest) {
        AdlsGen2Identity adlsGen2Identity = new AdlsGen2Identity();
        adlsGen2Identity.setManagedIdentity(storageIdentityRequest.getAdlsGen2().getManagedIdentity());
        return adlsGen2Identity;
    }

    private AccountMapping accountMappingRequestToAccountMapping(AccountMappingBase accountMappingRequest) {
        AccountMapping accountMapping = null;
        if (accountMappingRequest != null) {
            accountMapping = new AccountMapping();
            accountMapping.setGroupMappings(new HashMap<>(accountMappingRequest.getGroupMappings()));
            accountMapping.setUserMappings(new HashMap<>(accountMappingRequest.getUserMappings()));
        }
        return accountMapping;
    }

    private AccountMappingBase accountMappingToAccountMappingRequest(AccountMapping accountMapping) {
        AccountMappingBase accountMappingRequest = null;
        if (accountMapping != null) {
            accountMappingRequest = new AccountMappingBase();
            accountMappingRequest.setGroupMappings(new HashMap<>(accountMapping.getGroupMappings()));
            accountMappingRequest.setUserMappings(new HashMap<>(accountMapping.getUserMappings()));
        }
        return accountMappingRequest;
    }

}
