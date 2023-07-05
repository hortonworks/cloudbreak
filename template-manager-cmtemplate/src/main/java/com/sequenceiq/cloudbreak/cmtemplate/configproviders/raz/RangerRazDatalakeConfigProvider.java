package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

/**
 * Enables the Ranger Raz service.
 */
@Component
public class RangerRazDatalakeConfigProvider extends RangerRazBaseConfigProvider {
    private static final Set<String> ADDITIONAL_SERVICE_HOSTGROUPS = Set.of("master", "razhg");

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.DATALAKE == source.getStackType()
                && CMRepositoryVersionUtil.isRazConfigurationSupported(
                        source.getProductDetailsView().getCm().getVersion(), source.getCloudPlatform(), source.getStackType())
                && source.getGeneralClusterConfigs().isEnableRangerRaz()
                && TargetPlatform.PAAS.equals(TargetPlatform.getByCrn(source.getDatalakeView().get().getCrn()));
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)) {
            ApiClusterTemplateService coreSettings = createTemplate();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();

            return hostgroupViews.stream()
                    .filter(hg -> ADDITIONAL_SERVICE_HOSTGROUPS.contains(hg.getName().toLowerCase()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    public Set<String> getHostGroups(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getAdditionalServices(cmTemplateProcessor, source).keySet();
    }
}
