package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

/**
 * Enables the Ranger Raz service.
 */
@Component
public class RangerRazDatahubConfigProvider extends RangerRazBaseConfigProvider {

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD == source.getStackType()
                && (CloudPlatform.AWS == source.getCloudPlatform() || CloudPlatform.AZURE == source.getCloudPlatform())
                && CMRepositoryVersionUtil.isRazConfigurationSupportedInDatahub(source.getProductDetailsView().getCm())
                && source.getDatalakeView().isPresent()
                && source.getDatalakeView().get().isRazEnabled();
    }

}
