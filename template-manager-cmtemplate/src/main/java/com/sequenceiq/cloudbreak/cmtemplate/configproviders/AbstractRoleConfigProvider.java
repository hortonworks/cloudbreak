package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public abstract class AbstractRoleConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public Map<String, List<ApiClusterTemplateConfig>> getRoleConfigs(CmTemplateProcessor cmTemplate, TemplatePreparationObject source) {
        Optional<ApiClusterTemplateService> service = cmTemplate.getServiceByType(getServiceType());
        if (service.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();

        for (ApiClusterTemplateRoleConfigGroup rcg : ofNullable(service.get().getRoleConfigGroups()).orElseGet(List::of)) {
            if (getRoleTypes().contains(rcg.getRoleType())) {
                configs.put(rcg.getRefName(), getRoleConfigs(rcg.getRoleType(), source));
            }
        }

        return configs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    protected abstract List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source);

}
