package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class NifiRegistryVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        Integer volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
        return List.of(
                config("log_dir", buildSingleVolumePath(volumeCount, "nifi-registry-log"))
        );
    }

    @Override
    public String getServiceType() {
        return "NIFIREGISTRY";
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of("NIFI_REGISTRY_SERVER");
    }
}
