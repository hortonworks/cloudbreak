package com.sequenceiq.cloudbreak.template.filesystem.adls;

import static com.sequenceiq.common.api.filesystem.FileSystemType.ADLS;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.common.api.filesystem.FileSystemType;

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
