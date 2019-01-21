package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;

@Component
public class CloudStorageV4RequestToSpiFileSystemConverter extends AbstractConversionServiceAwareConverter<CloudStorageV4Request, SpiFileSystem> {

    @Override
    public SpiFileSystem convert(CloudStorageV4Request source) {
        CloudFileSystemView baseFileSystem = null;
        if (source.getAdls() != null) {
            baseFileSystem = getConversionService().convert(source.getAdls(), CloudAdlsView.class);
        } else if (source.getGcs() != null) {
            baseFileSystem = getConversionService().convert(source.getGcs(), CloudGcsView.class);
        } else if (source.getS3() != null) {
            baseFileSystem = getConversionService().convert(source.getS3(), CloudS3View.class);
        } else if (source.getWasb() != null) {
            baseFileSystem = getConversionService().convert(source.getWasb(), CloudWasbView.class);
        } else if (source.getAdlsGen2() != null) {
            baseFileSystem = getConversionService().convert(source.getAdlsGen2(), CloudAdlsGen2View.class);
        }
        return new SpiFileSystem(source.getName(), FileSystemType.valueOf(source.getType()), source.isDefaultFs(), baseFileSystem);
    }
}
