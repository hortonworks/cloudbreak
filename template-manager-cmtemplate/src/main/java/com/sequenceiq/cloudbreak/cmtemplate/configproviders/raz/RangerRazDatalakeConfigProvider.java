package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

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
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();

            return hostgroupViews.stream()
                    .filter(hg -> hg.getName().toLowerCase().equals("master"))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }
}
