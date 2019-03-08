package com.sequenceiq.cloudbreak.cmtemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.cloudera.api.swagger.model.ApiProductVersion;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.processor.ClusterDefinitionTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class CmTemplateProcessor implements ClusterDefinitionTextProcessor {
    private final ApiClusterTemplate cmTemplate;

    public CmTemplateProcessor(@Nonnull String cmTemplateText) {
        try {
            cmTemplate = JsonUtil.readValue(cmTemplateText, ApiClusterTemplate.class);
        } catch (IOException e) {
            throw new ClusterDefinitionProcessingException("Failed to parse cluster definition text.", e);
        }
    }

    @Override
    public ClusterManagerType getClusterManagerType() {
        return ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public Map<String, Set<String>> getComponentsByHostGroup() {
        Map<String, Set<String>> result = new HashMap<>();
        for (ApiClusterTemplateHostTemplate apiClusterTemplateHostTemplate : cmTemplate.getHostTemplates()) {
            Set<String> componentNames = new HashSet<>(apiClusterTemplateHostTemplate.getRoleConfigGroupsRefNames());
            result.put(apiClusterTemplateHostTemplate.getRefName(), componentNames);
        }
        return result;
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
        getServiceByType(serviceType).ifPresent(service -> configs.forEach(service::addServiceConfigsItem));
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

    public void resetProducts() {
        cmTemplate.setProducts(new ArrayList<>());
    }

    public void resetRepositories() {
        cmTemplate.setRepositories(new ArrayList<>());
    }

    public void addProduct(String product, String version) {
        ApiProductVersion productVersion = new ApiProductVersion();
        productVersion.setProduct(product);
        productVersion.setVersion(version);
        cmTemplate.addProductsItem(productVersion);
    }

    public void addRepositoryItem(String repositoriesItem) {
        cmTemplate.addRepositoriesItem(repositoriesItem);
    }

    public void setCmVersion(String cmVersion) {
        cmTemplate.setCmVersion(cmVersion);
    }

    public void setCdhVersion(String cdhVersion) {
        cmTemplate.setCdhVersion(cdhVersion);
    }
}
