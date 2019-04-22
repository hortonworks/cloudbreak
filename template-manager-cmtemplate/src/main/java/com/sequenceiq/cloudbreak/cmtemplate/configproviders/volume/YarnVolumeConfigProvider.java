package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class YarnVolumeConfigProvider extends AbstractVolumeConfigProvider {

    private static final String NODE_LOCAL_DIRS = "yarn_nodemanager_local_dirs";

    private static final String NODE_LOG_DIRS = "yarn_nodemanager_log_dirs";

    @Override
    List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();

        switch (roleType) {
            case "NODEMANAGER":
                String localDirs = getRoleTypeVariableName(hostGroupView.getName(), roleType, NODE_LOCAL_DIRS);
                roleConfigs.add(new ApiClusterTemplateConfig().name(NODE_LOCAL_DIRS).variable(localDirs));

                String logDirs = getRoleTypeVariableName(hostGroupView.getName(), roleType, NODE_LOG_DIRS);
                roleConfigs.add(new ApiClusterTemplateConfig().name(NODE_LOG_DIRS).variable(logDirs));
                break;
            default:
                break;
        }

        return roleConfigs;
    }

    @Override
    List<ApiClusterTemplateVariable> getVariables(String roleType, HostgroupView hostGroupView, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateVariable> variables = new ArrayList<>();

        switch (roleType) {
            case "NODEMANAGER":
                String localDirVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, NODE_LOCAL_DIRS);
                String localDirs = VolumeUtils.buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "nodemanager");
                variables.add(new ApiClusterTemplateVariable().name(localDirVar).value(localDirs));

                String logDirVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, NODE_LOG_DIRS);
                String loglDirs = VolumeUtils.buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "nodemanager/log");
                variables.add(new ApiClusterTemplateVariable().name(logDirVar).value(loglDirs));
                break;
            default:
                break;
        }

        return variables;
    }

    @Override
    public String getServiceType() {
        return "YARN";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("NODEMANAGER");
    }
}
