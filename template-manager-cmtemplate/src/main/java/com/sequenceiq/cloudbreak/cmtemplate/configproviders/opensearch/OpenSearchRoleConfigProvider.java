package com.sequenceiq.cloudbreak.cmtemplate.configproviders.opensearch;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class OpenSearchRoleConfigProvider extends AbstractRoleConfigProvider {

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String[] parts = roleType.toLowerCase().split("_");
        String lastElement = parts[parts.length - 1];
        Optional<HostgroupView> hostGroup = source.getHostGroupsWithComponent(roleType)
                .min(Comparator.comparing(HostgroupView::getVolumeCount));
        Integer minimumVolumeCount = hostGroup.isPresent() ? hostGroup.get().getVolumeCount() : 0;
        return List.of(
                config(
                        "semanticsearch_local_log_dir",
                        buildVolumePathStringZeroVolumeHandled(
                                minimumVolumeCount,
                                String.format("opensearch/logs/%s", lastElement)
                        )
                )
        );
    }

    @Override
    public String getServiceType() {
        return OpenSearchRoles.OPENSEARCH;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(
                OpenSearchRoles.OPENSEARCH_MASTER,
                OpenSearchRoles.OPENSEARCH_DATA,
                OpenSearchRoles.OPENSEARCH_ML,
                OpenSearchRoles.OPENSEARCH_COORDINATOR,
                OpenSearchRoles.OPENSEARCH_INGEST
        );
    }
}
