package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CmTemplateComponentConfigProviderProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateComponentConfigProviderProcessor.class);

    @Inject
    private List<CmTemplateComponentConfigProvider> providers;

    @Inject
    private List<AbstractRdsRoleConfigProvider> rdsRoleConfigProviders;

    public CmTemplateProcessor process(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject template) {
        for (CmTemplateComponentConfigProvider provider : providers) {
            String simpleName = provider.getClass().getSimpleName();
            Map<String, ApiClusterTemplateService> additionalServices = provider.getAdditionalServices(cmTemplateProcessor, template);
            LOGGER.info("Adding additional services: {} ", additionalServices);
            cmTemplateProcessor.extendTemplateWithAdditionalServices(additionalServices);
            if (provider.isConfigurationNeeded(cmTemplateProcessor, template)) {
                LOGGER.info("{} is configuring", simpleName);

                List<ApiClusterTemplateConfig> serviceConfigs = provider.getServiceConfigs(cmTemplateProcessor, template);
                LOGGER.info("Adding serviceconfigs: {} ", serviceConfigs);
                cmTemplateProcessor.addServiceConfigs(provider.getServiceType(), serviceConfigs);

                List<ApiClusterTemplateVariable> serviceConfigVariables = provider.getServiceConfigVariables(template);
                LOGGER.info("Adding serviceConfigVariables: {} ", serviceConfigVariables);
                cmTemplateProcessor.addVariables(serviceConfigVariables);

                Map<String, List<ApiClusterTemplateConfig>> roleConfigs = provider.getRoleConfigs(cmTemplateProcessor, template);
                LOGGER.info("Adding roleConfigs: {} ", roleConfigs);
                cmTemplateProcessor.addRoleConfigs(provider.getServiceType(), roleConfigs);

                List<ApiClusterTemplateVariable> roleConfigVariables = provider.getRoleConfigVariables(cmTemplateProcessor, template);
                LOGGER.info("Adding roleConfigVariables: {} ", roleConfigVariables);
                cmTemplateProcessor.addVariables(roleConfigVariables);
            } else {
                LOGGER.info("No need for configure the {}", simpleName);
            }
        }
        return cmTemplateProcessor;
    }

    public Map<String, Map<String, String>> getServiceConfigsToBeUpdatedDuringUpgrade(
            CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject template, String fromCmVersion, String toCmVersion) {
        Map<String, Map<String, String>> servicesNeedingServiceConfigUpdate = new HashMap<>();
        for (CmTemplateComponentConfigProvider provider : providers) {
            String simpleName = provider.getClass().getSimpleName();
            if (provider.isServiceConfigUpdateNeededForUpgrade(fromCmVersion, toCmVersion)) {
                LOGGER.info("{} is updating service configs during upgrade", simpleName);
                Map<String, String> updatedServiceConfigs = provider.getUpdatedServiceConfigForUpgrade(cmTemplateProcessor, template);
                if (!updatedServiceConfigs.isEmpty()) {
                    servicesNeedingServiceConfigUpdate.merge(provider.getServiceType(), updatedServiceConfigs, (oldMap, newMap) -> {
                        Map<String, String> mergedMap = new HashMap<>(oldMap);
                        mergedMap.putAll(newMap);
                        return mergedMap;
                    });
                } else {
                    LOGGER.warn("Service config map is empty from the {} during upgrade", simpleName);
                }
            } else {
                LOGGER.info("No need for update the service configs during upgrade for the {}", simpleName);
            }
        }
        return servicesNeedingServiceConfigUpdate;
    }

    public Table<String, String, String> collectDataConfigurations(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject template) {
        Table<String, String, String> cmServiceConfigTable = HashBasedTable.create();
        for (CmTemplateComponentConfigProvider provider : rdsRoleConfigProviders) {
            String simpleName = provider.getClass().getSimpleName();
            if (provider.isConfigurationNeeded(cmTemplateProcessor, template)) {
                LOGGER.info("{} is configuring", simpleName);
                List<ApiClusterTemplateConfig> serviceConfigs = provider.getServiceConfigs(cmTemplateProcessor, template);
                LOGGER.info("Adding serviceconfigs: {} ", serviceConfigs);
                for (ApiClusterTemplateConfig serviceConfig : serviceConfigs) {
                    cmServiceConfigTable.put(provider.getServiceType(), serviceConfig.getName(), serviceConfig.getValue());
                }
            }
        }
        return cmServiceConfigTable;
    }
}
