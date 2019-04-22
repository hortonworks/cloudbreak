package com.sequenceiq.cloudbreak.cmtemplate.configproviders.volume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HiveServer2ConfigProvider extends AbstractVolumeConfigProvider {
    @Override
    List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();

        switch (roleType) {
            case "HIVESERVER2":
                roleConfigs.add(new ApiClusterTemplateConfig().name("hive_hs2_config_safety_valve").variable("hive-hive_server2_wm_namespace"));
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
            case "HIVESERVER2":
                String uuid = templatePreparationObject.getGeneralClusterConfigs().getUuid();
                variables.add(new ApiClusterTemplateVariable().name("hive-hive_server2_wm_namespace")
                        .value("<property><name>hive.server2.wm.namespace</name><value>" + uuid + "</value></property>"));
                break;
            default:
                break;
        }

        return variables;
    }

    @Override
    public String getServiceType() {
        return "HIVE";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("HIVESERVER2");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
