package com.sequenceiq.cloudbreak.converter.spi;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.FileSystemValidationV4Request;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageParametersConverter;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class FileSystemValidationV4RequestToSpiFileSystemConverter {

    @Inject
    private CloudStorageParametersConverter cloudStorageParametersConverter;

    public SpiFileSystem convert(FileSystemValidationV4Request source) {
        List<CloudFileSystemView> cloudFileSystemViews = new ArrayList<>();
        if (source.getAdls() != null) {
            cloudFileSystemViews.add(cloudStorageParametersConverter.adlsToCloudView(source.getAdls()));
        } else if (source.getGcs() != null) {
            cloudFileSystemViews.add(cloudStorageParametersConverter.gcsToCloudView(source.getGcs()));
        } else if (source.getS3() != null) {
            cloudFileSystemViews.add(cloudStorageParametersConverter.s3ToCloudView(source.getS3()));
        } else if (source.getWasb() != null) {
            cloudFileSystemViews.add(cloudStorageParametersConverter.wasbToCloudView(source.getWasb()));
        } else if (source.getAdlsGen2() != null) {
            cloudFileSystemViews.add(cloudStorageParametersConverter.adlsGen2ToCloudView(source.getAdlsGen2()));
        }
        return new SpiFileSystem(source.getName(), FileSystemType.valueOf(source.getType()), cloudFileSystemViews);
    }
}
