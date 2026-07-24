package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupName;

/**
 * Enables the Ranger Raz service.
 */
@Component
public class RangerRazDatalakeConfigProvider extends RangerRazBaseConfigProvider {

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.DATALAKE == source.getStackType()
                && CMRepositoryVersionUtil.isRazConfigurationSupported(
                        source.getProductDetailsView().getCm().getVersion(), source.getCloudPlatform(), source.getStackType())
                && source.getGeneralClusterConfigs().isEnableRangerRaz();
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)) {
            ApiClusterTemplateService coreSettings = createTemplate();
            Set<String> targetGroups = resolveTargetGroups(cmTemplateProcessor, source);

            return source.getHostgroupViews().stream()
                    .filter(hg -> targetGroups.contains(hg.getName()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    /**
     * RAZ is co-located with the host group that runs Ranger Admin (the {@code master} group in legacy shapes,
     * the {@code gateway} group in the no-HDFS/HBase Enterprise shape), plus the {@code raz_scale_out} group when
     * the template defines it. Resolving by role placement avoids hardcoding group names.
     */
    private Set<String> resolveTargetGroups(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        Set<String> groups = new HashSet<>(cmTemplateProcessor.getHostGroupsWithComponent(RangerRoles.RANGER_ADMIN));
        source.getHostgroupViews().stream()
                .map(HostgroupView::getName)
                .filter(InstanceGroupName.RAZ_SCALE_OUT.getName()::equals)
                .forEach(groups::add);
        return groups;
    }

    public Set<String> getHostGroups(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getAdditionalServices(cmTemplateProcessor, source).keySet();
    }
}
