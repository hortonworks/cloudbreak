package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CmTemplateComponentConfigProviderProcessor {

    @Inject
    private List<CmTemplateComponentConfigProvider> providers;

    public CmTemplateProcessor process(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject template) {
        for (CmTemplateComponentConfigProvider provider : providers) {
            cmTemplateProcessor.extendTemplateWithAdditionalServices(provider.getAdditionalServices(cmTemplateProcessor, template));
            if (provider.isConfigurationNeeded(cmTemplateProcessor, template)) {
                cmTemplateProcessor.addServiceConfigs(provider.getServiceType(), provider.getRoleTypes(), provider.getServiceConfigs(template));
                cmTemplateProcessor.addVariables(provider.getServiceConfigVariables(template));
                cmTemplateProcessor.addRoleConfigs(provider.getServiceType(), provider.getRoleConfigs(cmTemplateProcessor, template));
                cmTemplateProcessor.addVariables(provider.getRoleConfigVariables(cmTemplateProcessor, template));
            }
        }
        return cmTemplateProcessor;
    }
}
