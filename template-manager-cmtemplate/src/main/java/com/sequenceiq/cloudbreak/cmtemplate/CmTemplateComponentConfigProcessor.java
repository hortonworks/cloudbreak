package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CmTemplateComponentConfigProcessor {

    @Inject
    private List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviderList;

    public CmTemplateProcessor process(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject template) {
        for (CmTemplateComponentConfigProvider provider : cmTemplateComponentConfigProviderList) {
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
