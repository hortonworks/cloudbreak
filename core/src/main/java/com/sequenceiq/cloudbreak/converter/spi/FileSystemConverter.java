package com.sequenceiq.cloudbreak.converter.spi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudEfsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AdlsGen2Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.EfsIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.GcsIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.EfsFileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class FileSystemConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemConverter.class);

    public SpiFileSystem fileSystemToSpi(FileSystem source) {
        List<CloudFileSystemView> cloudFileSystemViews = Collections.emptyList();
        if (source.getConfigurations() != null && source.getConfigurations().getValue() != null && source.getType() != FileSystemType.EFS) {
            cloudFileSystemViews = legacyConvertFromConfiguration(source);
        } else {
            cloudFileSystemViews = convertFromCloudStorage(source, cloudFileSystemViews);
        }

        if (source.getType() == FileSystemType.EFS && source.getConfigurations() != null && source.getConfigurations().getValue() != null) {
            Map<String, Object> efsParameters = null;

            try {
                efsParameters = source.getConfigurations().get(Map.class);
                if (efsParameters == null) {
                    efsParameters = new HashMap<>();
                }
            } catch (IOException ex) {
                LOGGER.error("Cannot get EFS parameters.", ex);
            }

            return new SpiFileSystem(source.getName(), source.getType(), cloudFileSystemViews, efsParameters);
        }

        return new SpiFileSystem(source.getName(), source.getType(), cloudFileSystemViews);
    }

    private List<CloudFileSystemView> convertFromCloudStorage(FileSystem source, List<CloudFileSystemView> cloudFileSystemViews) {
        CloudStorage cloudStorage = source.getCloudStorage();
        if (cloudStorage != null && cloudStorage.getCloudIdentities() != null && !cloudStorage.getCloudIdentities().isEmpty()) {
            cloudFileSystemViews = cloudStorage.getCloudIdentities().stream()
                    .map(cloudIdentity -> {
                        if (source.getType() != null) {
                            if (source.getType().isS3()) {
                                return cloudIdentityToS3View(cloudIdentity);
                            } else if (source.getType().isEfs()) {
                                return cloudIdentityToEfsView(cloudIdentity);
                            } else if (source.getType().isWasb()) {
                                return cloudIdentityToWasbView(cloudIdentity);
                            } else if (source.getType().isAdlsGen2()) {
                                return cloudIdentityToAdlsGen2View(cloudIdentity);
                            }  else if (source.getType().isGcs()) {
                                return cloudIdentityToGcsView(cloudIdentity);
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return cloudFileSystemViews;
    }

    private CloudGcsView cloudIdentityToGcsView(CloudIdentity cloudIdentity) {
        CloudGcsView cloudGcsView = new CloudGcsView(cloudIdentity.getIdentityType());
        GcsIdentity gcsIdentity = cloudIdentity.getGcsIdentity();
        if (Objects.isNull(gcsIdentity)) {
            LOGGER.warn("GCS identity is null. Identity type is {}", cloudIdentity.getIdentityType());
            return null;
        }
        cloudGcsView.setServiceAccountEmail(gcsIdentity.getServiceAccountEmail());
        return cloudGcsView;
    }

    private CloudS3View cloudIdentityToS3View(CloudIdentity cloudIdentity) {
        CloudS3View cloudS3View = new CloudS3View(cloudIdentity.getIdentityType());
        S3Identity s3Identity = cloudIdentity.getS3Identity();
        if (Objects.isNull(s3Identity)) {
            LOGGER.warn("S3 identity is null. Identity type is {}", cloudIdentity.getIdentityType());
            return null;
        }
        cloudS3View.setInstanceProfile(s3Identity.getInstanceProfile());
        return cloudS3View;
    }

    private CloudEfsView cloudIdentityToEfsView(CloudIdentity cloudIdentity) {
        CloudEfsView cloudEfsView = new CloudEfsView(cloudIdentity.getIdentityType());
        EfsIdentity efsIdentity = cloudIdentity.getEfsIdentity();
        if (Objects.isNull(efsIdentity)) {
            LOGGER.warn("EFS identity is null. Identity type is {}", cloudIdentity.getIdentityType());
            return null;
        }
        cloudEfsView.setInstanceProfile(efsIdentity.getInstanceProfile());
        return cloudEfsView;
    }

    private CloudFileSystemView cloudIdentityToWasbView(CloudIdentity cloudIdentity) {
        CloudWasbView cloudWasbView = new CloudWasbView(cloudIdentity.getIdentityType());
        cloudWasbView.setAccountName(cloudIdentity.getWasbIdentity().getAccountName());
        cloudWasbView.setAccountKey(cloudIdentity.getWasbIdentity().getAccountKey());
        cloudWasbView.setSecure(cloudIdentity.getWasbIdentity().isSecure());
        cloudWasbView.setResourceGroupName(cloudIdentity.getWasbIdentity().getStorageContainerName());
        return cloudWasbView;
    }

    private CloudFileSystemView cloudIdentityToAdlsGen2View(CloudIdentity cloudIdentity) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View(cloudIdentity.getIdentityType());
        AdlsGen2Identity adlsGen2Identity = cloudIdentity.getAdlsGen2Identity();
        if (Objects.isNull(adlsGen2Identity)) {
            LOGGER.warn("ADLS Gen2 managed identity with type {} is null.", cloudIdentity.getIdentityType());
            return null;
        }
        cloudAdlsGen2View.setManagedIdentity(adlsGen2Identity.getManagedIdentity());
        return cloudAdlsGen2View;
    }

    private List<CloudFileSystemView> legacyConvertFromConfiguration(FileSystem source) {
        try {
            CloudFileSystemView fileSystemView;
            if (source.getType().isAdls()) {
                AdlsFileSystem adlsFileSystem = source.getConfigurations().get(AdlsFileSystem.class);
                fileSystemView = convertAdlsLegacy(adlsFileSystem);
            } else if (source.getType().isGcs()) {
                GcsFileSystem gcsFileSystem = source.getConfigurations().get(GcsFileSystem.class);
                fileSystemView = convertGcsLegacy(gcsFileSystem);
            } else if (source.getType().isS3()) {
                S3FileSystem s3FileSystem = source.getConfigurations().get(S3FileSystem.class);
                fileSystemView = convertS3Legacy(s3FileSystem);
            } else if (source.getType().isEfs()) {
                EfsFileSystem efsFileSystem = source.getConfigurations().get(EfsFileSystem.class);
                fileSystemView = convertEfsLegacy(efsFileSystem);
            } else if (source.getType().isWasb()) {
                WasbFileSystem wasbFileSystem = source.getConfigurations().get(WasbFileSystem.class);
                fileSystemView = convertWasbLegacy(wasbFileSystem);
            } else if (source.getType().isAdlsGen2()) {
                AdlsGen2FileSystem adlsGen2FileSystem = source.getConfigurations().get(AdlsGen2FileSystem.class);
                fileSystemView = convertAdlsGen2Legacy(adlsGen2FileSystem);
            } else {
                return Collections.emptyList();
            }
            return List.of(fileSystemView);
        } catch (IOException e) {
            LOGGER.warn("Error occurred when tried to convert filesystem object: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private CloudWasbView convertWasbLegacy(WasbFileSystem source) {
        CloudWasbView cloudWasbView = new CloudWasbView(CloudIdentityType.LOG);
        cloudWasbView.setAccountName(source.getAccountName());
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setSecure(source.isSecure());
        cloudWasbView.setResourceGroupName(source.getStorageContainerName());
        return cloudWasbView;
    }

    private CloudS3View convertS3Legacy(S3FileSystem source) {
        CloudS3View cloudS3View = new CloudS3View(CloudIdentityType.LOG);
        cloudS3View.setInstanceProfile(source.getInstanceProfile());
        return cloudS3View;
    }

    private CloudEfsView convertEfsLegacy(EfsFileSystem source) {
        CloudEfsView cloudEfsView = new CloudEfsView(CloudIdentityType.LOG);
        cloudEfsView.setInstanceProfile(source.getInstanceProfile());
        return cloudEfsView;
    }

    private CloudGcsView convertGcsLegacy(GcsFileSystem source) {
        CloudGcsView cloudGcsView = new CloudGcsView(CloudIdentityType.LOG);
        cloudGcsView.setServiceAccountEmail(source.getServiceAccountEmail());
        return cloudGcsView;
    }

    private CloudAdlsView convertAdlsLegacy(AdlsFileSystem source) {
        CloudAdlsView cloudAdlsView = new CloudAdlsView(CloudIdentityType.LOG);
        cloudAdlsView.setTenantId(source.getTenantId());
        cloudAdlsView.setCredential(source.getCredential());
        cloudAdlsView.setAccountName(source.getAccountName());
        cloudAdlsView.setClientId(source.getClientId());
        return cloudAdlsView;
    }

    private CloudAdlsGen2View convertAdlsGen2Legacy(AdlsGen2FileSystem source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View(CloudIdentityType.LOG);
        cloudAdlsGen2View.setAccountName(source.getAccountName());
        cloudAdlsGen2View.setAccountKey(source.getAccountKey());
        cloudAdlsGen2View.setResourceGroupName(source.getStorageContainerName());
        cloudAdlsGen2View.setSecure(source.isSecure());
        cloudAdlsGen2View.setManagedIdentity(source.getManagedIdentity());
        return cloudAdlsGen2View;
    }
}
