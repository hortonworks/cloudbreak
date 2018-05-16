package com.sequenceiq.cloudbreak.blueprint.filesystem.wasb;

import static com.sequenceiq.cloudbreak.api.model.FileSystemType.WASB;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.filesystem.AbstractFileSystemConfigurator;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class WasbFileSystemConfigurator extends AbstractFileSystemConfigurator<WasbFileSystemConfiguration> {

    @Override
    public FileSystemType getFileSystemType() {
        return WASB;
    }

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential, WasbFileSystemConfiguration fsConfig) {
        return new ArrayList<>();
    }

}
