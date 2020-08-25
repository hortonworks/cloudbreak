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
public class RangerRazDatalakeConfigProvider extends RangerRazBaseConfigProvider {

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.DATALAKE == source.getStackType()
                && (CloudPlatform.AWS == source.getCloudPlatform() || CloudPlatform.AZURE == source.getCloudPlatform())
                && CMRepositoryVersionUtil.isRazConfigurationSupportedInDatalake(source.getProductDetailsView().getCm(), source.getCloudPlatform())
                && source.getGeneralClusterConfigs().isEnableRangerRaz();
    }

}
