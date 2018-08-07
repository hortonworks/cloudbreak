package com.sequenceiq.cloudbreak.blueprint.filesystem.adls;

import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.ADLS;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;

@Component
public class AdlsFileSystemConfigurator extends AbstractFileSystemConfigurator<AdlsFileSystemConfigurationsView> {

    @Override
    public FileSystemType getFileSystemType() {
        return ADLS;
    }

    @Override
    public String getProtocol() {
        return ADLS.getProtocol();
    }

}
