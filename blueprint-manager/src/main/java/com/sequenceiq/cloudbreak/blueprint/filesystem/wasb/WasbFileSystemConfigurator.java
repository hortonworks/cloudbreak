package com.sequenceiq.cloudbreak.blueprint.filesystem.wasb;

import static com.sequenceiq.cloudbreak.api.model.FileSystemType.WASB;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;

@Component
public class WasbFileSystemConfigurator extends AbstractFileSystemConfigurator<WasbFileSystemConfiguration> {

    @Override
    public FileSystemType getFileSystemType() {
        return WASB;
    }

}
