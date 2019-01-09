package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;

public class AbstractFileSystemConfiguratorImpl extends AbstractFileSystemConfigurator<AdlsFileSystemConfigurationsView> {

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential) {

        if (credential.getId() == 1L) {
            return Collections.singletonList(new FileSystemScriptConfig("file-system-config.script",
                    RecipeType.POST_AMBARI_START,
                    ExecutionType.ALL_NODES,
                    Collections.emptyMap()));
        } else if (credential.getId() == 2L) {
            return Collections.singletonList(new FileSystemScriptConfig("file-system-config-not-found",
                    RecipeType.POST_AMBARI_START,
                    ExecutionType.ALL_NODES,
                    Collections.emptyMap()));
        } else {
            return Collections.singletonList(new FileSystemScriptConfig("file-system-config.script",
                    RecipeType.POST_AMBARI_START,
                    ExecutionType.ALL_NODES,
                    Collections.singletonMap("replace", "newContent")));
        }
    }

    @Override
    public FileSystemType getFileSystemType() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }
}
