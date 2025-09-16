package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.List;
import java.util.Map;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

/**
 * Generates config for services and components (roles).
 * See {@link CmHostGroupRoleConfigProvider} for a generator that can create different configs depending on which host group the role is assigned to.
 * See {@link CmTemplateConfigInjector} for a generator that can generate global configs (eg. for some or all of the services or roles).
 */
public interface CmTemplateComponentConfigProvider {

    String getServiceType();

    List<String> getRoleTypes();

    default boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return false;
    }

    default List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    default List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        return List.of();
    }

    default Map<String, List<ApiClusterTemplateConfig>> getRoleConfigs(CmTemplateProcessor cmTemplate, TemplatePreparationObject source) {
        return Map.of();
    }

    default List<ApiClusterTemplateVariable> getRoleConfigVariables(CmTemplateProcessor cmTemplate, TemplatePreparationObject source) {
        return List.of();
    }

    default Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Map.of();
    }

    default boolean isServiceConfigUpdateNeededForUpgrade(String fromCmVersion, String toCmVersion) {
        return false;
    }

    default Map<String, String> getUpdatedServiceConfigForUpgrade(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Map.of();
    }

}
