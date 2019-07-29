package com.sequenceiq.cloudbreak.converter.spi;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;

@Component
public class FileSystemConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemConverter.class);

    @Inject
    private ConversionService conversionService;

    public SpiFileSystem fileSystemToSpi(FileSystem source) {
        CloudFileSystemView cloudFileSystemView = null;
        if (source.getConfigurations() != null && source.getConfigurations().getValue() != null) {
            cloudFileSystemView = legacyConvertFromConfiguration(source);
        } else {
            CloudStorage cloudStorage = source.getCloudStorage();
            if (cloudStorage != null) {
                // TODO: add support for multiple identities or multiple SpiFileSystems
                CloudIdentity cloudIdentity = cloudStorage.getCloudIdentities().get(0);
                if (cloudIdentity != null) {
                    if (source.getType().isS3()) {
                        cloudFileSystemView = cloudIdentityToS3View(cloudIdentity);
                    } else if (source.getType().isWasb()) {
                        cloudFileSystemView = cloudIdentityToWasbView(cloudIdentity);
                    }
                }
            }
        }
        return new SpiFileSystem(source.getName(), source.getType(), cloudFileSystemView);
    }

    private CloudS3View cloudIdentityToS3View(CloudIdentity cloudIdentity) {
        CloudS3View cloudS3View = new CloudS3View();
        cloudS3View.setInstanceProfile(cloudIdentity.getS3Identity().getInstanceProfile());
        return cloudS3View;
    }

    private CloudFileSystemView cloudIdentityToWasbView(CloudIdentity cloudIdentity) {
        CloudWasbView cloudWasbView = new CloudWasbView();
        cloudWasbView.setAccountName(cloudIdentity.getWasbIdentity().getAccountName());
        cloudWasbView.setAccountKey(cloudIdentity.getWasbIdentity().getAccountKey());
        cloudWasbView.setSecure(cloudIdentity.getWasbIdentity().isSecure());
        cloudWasbView.setResourceGroupName(cloudIdentity.getWasbIdentity().getStorageContainerName());
        return cloudWasbView;
    }

    private CloudFileSystemView legacyConvertFromConfiguration(FileSystem source) {
        try {
            if (source.getType().isAdls()) {
                return conversionService.convert(source.getConfigurations().get(AdlsFileSystem.class), CloudAdlsView.class);
            } else if (source.getType().isGcs()) {
                return conversionService.convert(source.getConfigurations().get(GcsFileSystem.class), CloudGcsView.class);
            } else if (source.getType().isS3()) {
                return conversionService.convert(source.getConfigurations().get(S3FileSystem.class), CloudS3View.class);
            } else if (source.getType().isWasb()) {
                return conversionService.convert(source.getConfigurations().get(WasbFileSystem.class), CloudWasbView.class);
            }
        } catch (IOException e) {
            LOGGER.warn("Error occurred when tried to convert filesystem object: {}", e.getMessage());
        }
        return null;
    }
}
