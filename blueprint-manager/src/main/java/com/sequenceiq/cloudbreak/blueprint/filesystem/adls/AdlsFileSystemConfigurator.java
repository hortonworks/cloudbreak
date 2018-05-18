package com.sequenceiq.cloudbreak.blueprint.filesystem.adls;

import static com.sequenceiq.cloudbreak.api.model.FileSystemType.ADLS;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;

@Component
public class AdlsFileSystemConfigurator extends AbstractFileSystemConfigurator<AdlsFileSystemConfiguration> {

    @Override
    public FileSystemType getFileSystemType() {
        return ADLS;
    }

}
