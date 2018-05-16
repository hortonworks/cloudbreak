package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.domain.Credential;

public class AbstractFileSystemConfiguratorImpl extends AbstractFileSystemConfigurator<FileSystemConfiguration> {

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential, FileSystemConfiguration fsConfig) {

        if (credential.getId() == 1) {
            return Arrays.asList(new FileSystemScriptConfig("file-system-config.script",
                    RecipeType.POST_AMBARI_START,
                    ExecutionType.ALL_NODES,
                    Collections.emptyMap()));
        } else if (credential.getId() == 2) {
            return Arrays.asList(new FileSystemScriptConfig("file-system-config-not-found",
                    RecipeType.POST_AMBARI_START,
                    ExecutionType.ALL_NODES,
                    Collections.emptyMap()));
        } else {
            return Arrays.asList(new FileSystemScriptConfig("file-system-config.script",
                    RecipeType.POST_AMBARI_START,
                    ExecutionType.ALL_NODES,
                    Collections.singletonMap("replace", "newContent")));
        }
    }

    @Override
    public FileSystemType getFileSystemType() {
        return null;
    }
}
