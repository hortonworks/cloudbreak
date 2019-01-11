package com.sequenceiq.cloudbreak.template.filesystem.wasb;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType.WASB;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;

@Component
public class WasbFileSystemConfigurator extends AbstractFileSystemConfigurator<WasbFileSystemConfigurationsView> {

    @Override
    public FileSystemType getFileSystemType() {
        return WASB;
    }

    @Override
    public String getProtocol() {
        return WASB.getProtocol();
    }

}
