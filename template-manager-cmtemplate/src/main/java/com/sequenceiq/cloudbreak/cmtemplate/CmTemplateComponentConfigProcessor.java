package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CmTemplateComponentConfigProcessor {

    @Inject
    private List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviderList;

    public CmTemplateProcessor process(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        for (CmTemplateComponentConfigProvider provider : getCmTemplateComponentConfigProviderList()) {
            if (provider.specialCondition(cmTemplateProcessor, source)) {
                cmTemplateProcessor.addServiceConfigs(provider.getServiceType(), provider.getRoleType(), provider.getServiceConfigs(source));
                cmTemplateProcessor.addVariables(provider.getVariables(source));
            }
        }
        return cmTemplateProcessor;
    }

    public List<CmTemplateComponentConfigProvider> getCmTemplateComponentConfigProviderList() {
        return cmTemplateComponentConfigProviderList;
    }
}
