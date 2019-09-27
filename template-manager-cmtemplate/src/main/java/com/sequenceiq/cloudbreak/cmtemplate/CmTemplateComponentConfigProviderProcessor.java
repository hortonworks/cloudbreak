package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CmTemplateComponentConfigProviderProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateComponentConfigProviderProcessor.class);

    @Inject
    private List<CmTemplateComponentConfigProvider> providers;

    public CmTemplateProcessor process(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject template) {
        for (CmTemplateComponentConfigProvider provider : providers) {
            cmTemplateProcessor.extendTemplateWithAdditionalServices(provider.getAdditionalServices(cmTemplateProcessor, template));
            if (provider.isConfigurationNeeded(cmTemplateProcessor, template)) {
                LOGGER.info("{} is configuring", provider.getClass().getSimpleName());
                cmTemplateProcessor.addServiceConfigs(provider.getServiceType(), provider.getRoleTypes(),
                        provider.getServiceConfigs(cmTemplateProcessor, template));
                cmTemplateProcessor.addVariables(provider.getServiceConfigVariables(template));
                cmTemplateProcessor.addRoleConfigs(provider.getServiceType(), provider.getRoleConfigs(cmTemplateProcessor, template));
                cmTemplateProcessor.addVariables(provider.getRoleConfigVariables(cmTemplateProcessor, template));
            } else {
                LOGGER.info("No need for configure the {}", provider.getClass().getSimpleName());
            }
        }
        return cmTemplateProcessor;
    }
}
