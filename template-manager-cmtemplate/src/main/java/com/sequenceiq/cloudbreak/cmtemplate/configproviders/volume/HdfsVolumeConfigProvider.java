package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsVolumeConfigProvider extends AbstractVolumeConfigProvider {

    @Override
    List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        String variable = getRoleTypeVariableName(hostGroupView.getName(), roleType);
        switch (roleType) {
            case "DATANODE":
                roleConfigs.add(new ApiClusterTemplateConfig().name("dfs_data_dir_list").variable(variable));
                break;
            case "NAMENODE":
                roleConfigs.add(new ApiClusterTemplateConfig().name("dfs_name_dir_list").variable(variable));
                break;
            default:
                break;
        }
        return roleConfigs;
    }

    @Override
    List<ApiClusterTemplateVariable> getVariables(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateVariable> variables = new ArrayList<>();
        String variable = getRoleTypeVariableName(hostGroupView.getName(), roleType);
        String dirs = VolumeUtils.buildVolumePathString(hostGroupView.getVolumeCount(), roleType.toLowerCase());
        variables.add(new ApiClusterTemplateVariable().name(variable).value(dirs));
        return variables;
    }

    @Override
    public String getServiceType() {
        return "HDFS";
    }

    @Override
    public List<String> getRoleTypes() {
        return Arrays.asList("NAMENODE", "DATANODE");
    }

}
