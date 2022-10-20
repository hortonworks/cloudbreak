package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.type.ExecutionType;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.common.model.FileSystemType;

public class AbstractFileSystemConfiguratorImpl extends AbstractFileSystemConfigurator<AdlsFileSystemConfigurationsView> {

    @Override
    protected List<FileSystemScriptConfig> getScriptConfigs(Credential credential) {

        if ("crn1".equals(credential.getCrn())) {
            return Collections.singletonList(new FileSystemScriptConfig("file-system-config.script",
                    RecipeType.POST_CLOUDERA_MANAGER_START,
                    ExecutionType.ALL_NODES,
                    Collections.emptyMap()));
        } else if ("crn2".equals(credential.getCrn())) {
            return Collections.singletonList(new FileSystemScriptConfig("file-system-config-not-found",
                    RecipeType.POST_CLOUDERA_MANAGER_START,
                    ExecutionType.ALL_NODES,
                    Collections.emptyMap()));
        } else {
            return Collections.singletonList(new FileSystemScriptConfig("file-system-config.script",
                    RecipeType.POST_CLOUDERA_MANAGER_START,
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
