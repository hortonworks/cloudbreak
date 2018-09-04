package com.sequenceiq.cloudbreak.template.filesystem.abfs;

import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.ABFS;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;

@Component
public class AbfsFileSystemConfigurator extends AbstractFileSystemConfigurator<AbfsFileSystemConfigurationsView> {

    @Override
    public FileSystemType getFileSystemType() {
        return ABFS;
    }

    @Override
    public String getProtocol() {
        return ABFS.getProtocol();
    }

}
