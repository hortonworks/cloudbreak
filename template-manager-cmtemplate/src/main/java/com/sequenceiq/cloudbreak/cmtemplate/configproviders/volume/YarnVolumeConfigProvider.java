package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class YarnVolumeConfigProvider extends AbstractVolumeConfigProvider {
    @Override
    List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        String variable = getRoleTypeVariableName(hostGroupView.getName(), roleType);
        switch (roleType) {
            case "NODEMANAGER":
                roleConfigs.add(new ApiClusterTemplateConfig().name("yarn_nodemanager_local_dirs").variable(variable));
                roleConfigs.add(new ApiClusterTemplateConfig().name("yarn_nodemanager_log_dirs").variable(variable));
                break;
            default:
                break;
        }
        return roleConfigs;
    }

    @Override
    public String getServiceType() {
        return "YARN";
    }

    @Override
    public List<String> getRoleTypes() {
        return Arrays.asList("NODEMANAGER");
    }
}
