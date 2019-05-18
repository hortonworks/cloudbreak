package com.sequenceiq.cloudbreak.template.filesystem.wasb;

import static com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType.WASB;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;
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
