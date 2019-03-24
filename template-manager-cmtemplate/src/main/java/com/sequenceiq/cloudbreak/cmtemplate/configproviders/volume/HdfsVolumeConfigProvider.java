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

    private static final String DFS_DATA_DIRS = "dfs_data_dir_list";

    private static final String DFS_NAME_DIRS = "dfs_name_dir_list";

    private static final String DFS_CHECK_DIRS = "fs_checkpoint_dir_list";

    @Override
    List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();

        switch (roleType) {
            case "DATANODE":
                String dataDir = getRoleTypeVariableName(hostGroupView.getName(), roleType, DFS_DATA_DIRS);
                roleConfigs.add(new ApiClusterTemplateConfig().name(DFS_DATA_DIRS).variable(dataDir));
                break;
            case "NAMENODE":
                String nameDir = getRoleTypeVariableName(hostGroupView.getName(), roleType, DFS_NAME_DIRS);
                roleConfigs.add(new ApiClusterTemplateConfig().name(DFS_NAME_DIRS).variable(nameDir));
                break;
            case "SECONDARYNAMENODE":
                String checkDir = getRoleTypeVariableName(hostGroupView.getName(), roleType, DFS_CHECK_DIRS);
                roleConfigs.add(new ApiClusterTemplateConfig().name(DFS_CHECK_DIRS).variable(checkDir));
                break;
            default:
                break;
        }

        return roleConfigs;
    }

    @Override
    List<ApiClusterTemplateVariable> getVariables(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateVariable> variables = new ArrayList<>();

        switch (roleType) {
            case "DATANODE":
                String dataDirVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, DFS_DATA_DIRS);
                String dataDirs = VolumeUtils.buildVolumePathString(hostGroupView.getVolumeCount(), "datanode");
                variables.add(new ApiClusterTemplateVariable().name(dataDirVar).value(dataDirs));
                break;
            case "NAMENODE":
                String nameDirVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, DFS_NAME_DIRS);
                String nameDirs = VolumeUtils.buildVolumePathString(hostGroupView.getVolumeCount(), "namenode");
                variables.add(new ApiClusterTemplateVariable().name(nameDirVar).value(nameDirs));
                break;
            case "SECONDARYNAMENODE":
                String checkDirVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, DFS_CHECK_DIRS);
                String checkDirs = VolumeUtils.buildVolumePathString(hostGroupView.getVolumeCount(), "namesecondary");
                variables.add(new ApiClusterTemplateVariable().name(checkDirVar).value(checkDirs));
                break;
            default:
                break;
        }

        return variables;
    }

    @Override
    public String getServiceType() {
        return "HDFS";
    }

    @Override
    public List<String> getRoleTypes() {
        return Arrays.asList("NAMENODE", "DATANODE", "SECONDARYNAMENODE");
    }

}
