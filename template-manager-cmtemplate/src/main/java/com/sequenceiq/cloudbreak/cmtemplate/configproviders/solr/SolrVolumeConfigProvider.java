package com.sequenceiq.cloudbreak.cmtemplate.configproviders.solr;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class SolrVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        Integer volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
        if (SolrRoles.SOLR_SERVER.equals(roleType)) {
            return List.of(
                    config("solr_data_dir", buildSingleVolumePath(volumeCount, "solr"))
            );
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return SolrRoles.SOLR;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(SolrRoles.SOLR_SERVER);
    }

    @Override
    public boolean shouldSplit(ServiceComponent serviceComponent) {
        return true;
    }
}
