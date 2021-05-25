package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enables the Ranger Raz service.
 */
@Component
public class RangerRazDatalakeConfigProvider extends RangerRazBaseConfigProvider {

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return isDatalakeConfigForRazIsRequired(source);
    }

    public static boolean isDatalakeConfigForRazIsRequired(TemplatePreparationObject source) {
        return StackType.DATALAKE == source.getStackType()
                && (CloudPlatform.AWS == source.getCloudPlatform() || CloudPlatform.AZURE == source.getCloudPlatform())
                && CMRepositoryVersionUtil.isRazConfigurationSupportedInDatalake(source.getProductDetailsView().getCm(), source.getCloudPlatform())
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
