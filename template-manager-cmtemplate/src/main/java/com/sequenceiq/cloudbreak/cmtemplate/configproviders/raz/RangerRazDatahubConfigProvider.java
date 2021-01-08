package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;
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
public class RangerRazDatahubConfigProvider extends RangerRazBaseConfigProvider {

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD == source.getStackType()
                && (CloudPlatform.AWS == source.getCloudPlatform() || CloudPlatform.AZURE == source.getCloudPlatform())
                && source.getProductDetailsView() != null
                && source.getProductDetailsView().getCm() != null
                && CMRepositoryVersionUtil.isRazConfigurationSupportedInDatahub(source.getProductDetailsView().getCm())
                && source.getDatalakeView().isPresent()
                && source.getDatalakeView().get().isRazEnabled();
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)) {
            ApiClusterTemplateService coreSettings = createTemplate();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();

            return hostgroupViews.stream()
                    .filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }
}
