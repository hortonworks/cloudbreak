package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class FileSystemViewFactory {

    private FileSystemViewFactory() {
    }

    public static FileSystemView convertToFileSystem(FileSystemConfigurationView fileSystemConfigurationView) {
        FileSystemConfiguration fileSystemConfiguration = fileSystemConfigurationView.getFileSystemConfiguration();
        if (fileSystemConfiguration != null) {
            if (fileSystemConfiguration instanceof GcsFileSystemConfiguration) {
                return new GcsFileSystemView(fileSystemConfigurationView);
            } else if (fileSystemConfiguration instanceof WasbFileSystemConfiguration) {
                return new WasbFileSystemView(fileSystemConfigurationView);
            } else if (fileSystemConfiguration instanceof AdlsFileSystemConfiguration) {
                return new AdlsFileSystemView(fileSystemConfigurationView);
            }
        }
        String message = String.format("Could not cast FileSystem '%s' to FileSystemView because the object class was not implemented: %s",
                fileSystemConfiguration, fileSystemConfiguration.getClass());
        throw new CloudbreakServiceException(message);
    }
}
