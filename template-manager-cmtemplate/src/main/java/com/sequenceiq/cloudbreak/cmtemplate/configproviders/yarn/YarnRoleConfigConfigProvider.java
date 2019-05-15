package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class YarnRoleConfigConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String NODE_LOCAL_DIRS = "yarn_nodemanager_local_dirs";

    private static final String NODE_LOG_DIRS = "yarn_nodemanager_log_dirs";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case YarnRoles.NODEMANAGER:
                return List.of(
                        config(NODE_LOCAL_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "nodemanager")),
                        config(NODE_LOG_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "nodemanager/log"))
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
    public List<String> getRoleTypes() {
        return List.of(YarnRoles.NODEMANAGER);
    }
}
