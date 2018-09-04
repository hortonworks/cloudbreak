package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.template.filesystem.abfs.AbfsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.wasb.WasbFileSystemConfigurationsView;

@Service
public class FileSystemConfigurationsViewProvider {

    public BaseFileSystemConfigurationsView propagateConfigurationsView(FileSystem source) throws IOException {
        Set<StorageLocationView> locations = new HashSet<>();
        try {
            if (source.getLocations() != null && source.getLocations().getValue() != null) {
                StorageLocations storageLocations = source.getLocations().get(StorageLocations.class);
                if (storageLocations != null) {
                    for (StorageLocation location : storageLocations.getLocations()) {
                        locations.add(new StorageLocationView(location));
                    }
                }
            } else {
                locations = new HashSet<>();
            }
        } catch (IOException e) {
            locations = new HashSet<>();
        }
        return getBaseFileSystemConfigurationsView(source, locations);
    }

    private BaseFileSystemConfigurationsView getBaseFileSystemConfigurationsView(FileSystem source, Set<StorageLocationView> locations) throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationsView = null;
        if (source.getType().isAdls()) {
            fileSystemConfigurationsView =
                    new AdlsFileSystemConfigurationsView(source.getConfigurations().get(AdlsFileSystem.class), locations, source.isDefaultFs());
        } else if (source.getType().isGcs()) {
            fileSystemConfigurationsView =
                    new GcsFileSystemConfigurationsView(source.getConfigurations().get(GcsFileSystem.class), locations, source.isDefaultFs());
        } else if (source.getType().isS3()) {
            fileSystemConfigurationsView =
                    new S3FileSystemConfigurationsView(source.getConfigurations().get(S3FileSystem.class), locations, source.isDefaultFs());
        } else if (source.getType().isWasb()) {
            fileSystemConfigurationsView =
                    new WasbFileSystemConfigurationsView(source.getConfigurations().get(WasbFileSystem.class), locations, source.isDefaultFs());
        } else if (source.getType().isAbfs()) {
            fileSystemConfigurationsView =
                    new AbfsFileSystemConfigurationsView(source.getConfigurations().get(AbfsFileSystem.class), locations, source.isDefaultFs());
        }
        return fileSystemConfigurationsView;
    }
}
