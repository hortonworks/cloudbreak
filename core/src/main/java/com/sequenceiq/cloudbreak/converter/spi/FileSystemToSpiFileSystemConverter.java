package com.sequenceiq.cloudbreak.converter.spi;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3.S3FileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FileSystemToSpiFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystem, SpiFileSystem> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemToSpiFileSystemConverter.class);

    @Override
    public SpiFileSystem convert(FileSystem source) {
        CloudFileSystemView cloudFileSystemView = null;
        try {
            if (source.getType().isAdls()) {
                cloudFileSystemView = getConversionService().convert(source.getConfigurations().get(AdlsFileSystem.class), CloudAdlsView.class);
            } else if (source.getType().isGcs()) {
                cloudFileSystemView = getConversionService().convert(source.getConfigurations().get(GcsFileSystem.class), CloudGcsView.class);
            } else if (source.getType().isS3()) {
                cloudFileSystemView = getConversionService().convert(source.getConfigurations().get(S3FileSystem.class), CloudS3View.class);
            } else if (source.getType().isWasb()) {
                cloudFileSystemView = getConversionService().convert(source.getConfigurations().get(WasbFileSystem.class), CloudWasbView.class);
            }
        } catch (IOException e) {
            LOGGER.warn("Error occurred when tried to convert filesystem object: {}", e.getMessage());
        }
        return new SpiFileSystem(source.getName(), source.getType(), source.isDefaultFs(), cloudFileSystemView);
    }

}
