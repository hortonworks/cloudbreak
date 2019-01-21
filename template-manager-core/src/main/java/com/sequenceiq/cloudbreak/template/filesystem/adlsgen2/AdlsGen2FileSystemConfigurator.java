package com.sequenceiq.cloudbreak.template.filesystem.adlsgen2;

import static com.sequenceiq.cloudbreak.services.filesystem.FileSystemType.ADLS_GEN_2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;

@Component
public class AdlsGen2FileSystemConfigurator extends AbstractFileSystemConfigurator<AdlsGen2FileSystemConfigurationsView> {

    @Override
    public FileSystemType getFileSystemType() {
        return ADLS_GEN_2;
    }

    @Override
    public String getProtocol() {
        return ADLS_GEN_2.getProtocol();
    }

}
