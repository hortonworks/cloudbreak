package com.sequenceiq.cloudbreak.blueprint.filesystem.wasb;

import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.WASB;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;

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
