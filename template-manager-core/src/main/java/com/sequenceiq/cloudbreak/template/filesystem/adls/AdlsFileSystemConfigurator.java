package com.sequenceiq.cloudbreak.template.filesystem.adls;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType.ADLS;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemType;
import com.sequenceiq.cloudbreak.template.filesystem.AbstractFileSystemConfigurator;

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
