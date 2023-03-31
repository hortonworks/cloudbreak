package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM_SERVER;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class EfmVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    static final String CM_LOG_DIR = "log_dir";

    static final String EFM_LOG_DIR = "logs/efm";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        return List.of(config(CM_LOG_DIR, buildSingleVolumePath(Optional.ofNullable(hostGroupView).map(HostgroupView::getVolumeCount).orElse(0), EFM_LOG_DIR)));
    }

    @Override
    public String getServiceType() {
        return EFM;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(EFM_SERVER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return false;
    }
}
