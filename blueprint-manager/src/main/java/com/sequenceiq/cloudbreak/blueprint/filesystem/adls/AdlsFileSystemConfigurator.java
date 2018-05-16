package com.sequenceiq.cloudbreak.blueprint.filesystem.adls;

import static com.sequenceiq.cloudbreak.api.model.FileSystemType.ADLS;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class AdlsFileSystemConfigurator extends AbstractFileSystemConfigurator<AdlsFileSystemConfiguration> {

    @Override
    public FileSystemType getFileSystemType() {
        return ADLS;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential, AdlsFileSystemConfiguration fsConfig) {
        return new ArrayList<>();
    }

}
