package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildEphemeralVolumePathString;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class YarnVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    private static final String NODE_LOCAL_DIRS = "yarn_nodemanager_local_dirs";

    private static final String NODE_LOG_DIRS = "yarn_nodemanager_log_dirs";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case YarnRoles.NODEMANAGER:
                Integer volumeCount = 0;
                Integer temporaryStorageVolumeCount = 0;
                if (hostGroupView != null) {
                    volumeCount = hostGroupView.getVolumeCount();
                    if (hostGroupView.getTemporaryStorageVolumeCount() != null) {
                        temporaryStorageVolumeCount = hostGroupView.getTemporaryStorageVolumeCount();
                    }
                }
                String localDirsVolumePath;
                if (hostGroupView != null && hostGroupView.getTemporaryStorage() == TemporaryStorage.EPHEMERAL_VOLUMES && temporaryStorageVolumeCount != 0) {
                    localDirsVolumePath = buildEphemeralVolumePathString(temporaryStorageVolumeCount, "nodemanager");
                } else {
                    localDirsVolumePath = buildVolumePathStringZeroVolumeHandled(volumeCount, "nodemanager");
                }
                return List.of(
                        config(NODE_LOCAL_DIRS, localDirsVolumePath),
                        config(NODE_LOG_DIRS, buildVolumePathStringZeroVolumeHandled(volumeCount, "nodemanager/log"))
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return YarnRoles.YARN;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(YarnRoles.NODEMANAGER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }
}
