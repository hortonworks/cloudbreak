package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
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
                Integer volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
                return List.of(
                        config(NODE_LOCAL_DIRS, buildVolumePathStringZeroVolumeHandled(volumeCount, "nodemanager")),
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
