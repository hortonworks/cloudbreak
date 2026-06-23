package com.sequenceiq.cloudbreak.template.filesystem.wasb;

import static com.sequenceiq.common.model.FileSystemType.WASB;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.common.model.FileSystemType;

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
