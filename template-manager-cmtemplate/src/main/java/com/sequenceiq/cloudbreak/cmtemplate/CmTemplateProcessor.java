package com.sequenceiq.cloudbreak.cmtemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class CmTemplateProcessor {
    private final ApiClusterTemplate cmTemplate;

    public CmTemplateProcessor(@Nonnull String cmTemplateText) {
        try {
            cmTemplate = JsonUtil.readValue(cmTemplateText, ApiClusterTemplate.class);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse blueprint text.", e);
        }
    }

    public void addInstantiator(String clusterName) {
        ApiClusterTemplateInstantiator instantiator = cmTemplate.getInstantiator();
        if (instantiator == null) {
            instantiator = new ApiClusterTemplateInstantiator();
            instantiator.setClusterName(clusterName);
        }
        cmTemplate.setInstantiator(instantiator);
    }

    public void addVariables(List<ApiClusterTemplateVariable> vars) {
        for (ApiClusterTemplateVariable v : vars) {
            cmTemplate.getInstantiator().addVariablesItem(v);
        }
    }

    public void addServiceConfigs(String serviceType, String roleType, List<ApiClusterTemplateConfig> configs) {
        getServiceByType(serviceType).stream().forEach(service -> configs.stream().forEach(config -> service.addServiceConfigsItem(config)));
    }

    public boolean isRoleTypePresentInService(String serviceType, String roleType) {
        return getServiceByType(serviceType).filter(acts -> isRoleTypePresent(acts, roleType)).isPresent();

    }

    private boolean isRoleTypePresent(ApiClusterTemplateService apiClusterTemplateService, String roleType) {
        for (ApiClusterTemplateRoleConfigGroup rcg : apiClusterTemplateService.getRoleConfigGroups()) {
            if (roleType.equalsIgnoreCase(rcg.getRoleType())) {
                return true;
            }
        }
        return false;
    }

    private Optional<ApiClusterTemplateService> getServiceByType(String serviceType) {
        for (ApiClusterTemplateService service : cmTemplate.getServices()) {
            if (serviceType.equalsIgnoreCase(service.getServiceType())) {
                return Optional.of(service);
            }
        }
        return Optional.empty();
    }

    public ApiClusterTemplate getTemplate() {
        return cmTemplate;
    }

    public void addHosts(Map<String, List<Map<String, String>>> hostGroupMappings) {
        hostGroupMappings.forEach((hostGroup, hostAttributes) -> hostAttributes.forEach(
                attr -> cmTemplate.getInstantiator().addHostsItem(new ApiClusterTemplateHostInfo().hostName(attr.get("fqdn")).hostTemplateRefName(hostGroup))
        ));
    }
}
