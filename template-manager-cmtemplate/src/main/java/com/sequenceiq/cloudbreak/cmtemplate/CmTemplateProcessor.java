package com.sequenceiq.cloudbreak.cmtemplate;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.NotImplementedException;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateClusterSpec;
import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroupInfo;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.cloudera.api.swagger.model.ApiConfigureForKerberosArguments;
import com.cloudera.api.swagger.model.ApiDataContextRef;
import com.cloudera.api.swagger.model.ApiProductVersion;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;

public class CmTemplateProcessor implements BlueprintTextProcessor {

    private final ApiClusterTemplate cmTemplate;

    public CmTemplateProcessor(@Nonnull String cmTemplateText) {
        try {
            cmTemplate = JsonUtil.readValue(cmTemplateText, ApiClusterTemplate.class);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse blueprint text.", e);
        }
    }

    @Override
    public ClusterManagerType getClusterManagerType() {
        return ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public BlueprintTextProcessor replaceConfiguration(String s, String descriptor) {
        throw new NotImplementedException("");
    }

    @Override
    public BlueprintTextProcessor addComponentToHostgroups(String component, Predicate<String> addToHostgroup) {
        throw new NotImplementedException("");
    }

    @Override
    public boolean isComponentExistsInHostGroup(String component, String hostGroup) {
        Set<String> componentsInHostGroup = getComponentsInHostGroup(hostGroup);
        return componentsInHostGroup.stream().anyMatch(component::equals);
    }

    @Override
    public BlueprintTextProcessor addComponentToHostgroups(String component, Collection<String> addToHostGroups) {
        throw new NotImplementedException("");
    }

    @Override
    public Set<String> getHostGroupsWithComponent(String components) {
        throw new NotImplementedException("");
    }

    @Override
    public String asText() {
        return cmTemplate.toString();
    }

    @Override
    public BlueprintTextProcessor setSecurityType(String kerberos) {
        throw new NotImplementedException("");
    }

    @Override
    public BlueprintTextProcessor extendBlueprintGlobalConfiguration(SiteConfigurations configs, boolean forced) {
        throw new NotImplementedException("");
    }

    @Override
    public Set<String> getComponentsInHostGroup(String hostGroup) {
        Set<String> roleConfigRefNamesInHostTemplate = cmTemplate.getHostTemplates().stream()
                .filter(hostTemplate -> hostTemplate.getRefName().equals(hostGroup))
                .map(ApiClusterTemplateHostTemplate::getRoleConfigGroupsRefNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return cmTemplate.getServices().stream()
                .map(ApiClusterTemplateService::getRoleConfigGroups)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(roleConfigGroup -> roleConfigRefNamesInHostTemplate.contains(roleConfigGroup.getRefName()))
                .map(ApiClusterTemplateRoleConfigGroup::getRoleType)
                .collect(Collectors.toSet());
    }

    @Override
    public BlueprintTextProcessor extendBlueprintHostGroupConfiguration(HostgroupConfigurations hostgroupConfigurations, boolean b) {
        throw new NotImplementedException("");
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

    public void addInstantiator(ClouderaManagerRepo clouderaManagerRepoDetails, TemplatePreparationObject templatePreparationObject, String sdxContextName) {
        ApiClusterTemplateInstantiator instantiator = cmTemplate.getInstantiator();
        if (instantiator == null) {
            instantiator = new ApiClusterTemplateInstantiator();
            instantiator.setClusterName(templatePreparationObject.getGeneralClusterConfigs().getClusterName());
        }
        if (Objects.nonNull(clouderaManagerRepoDetails)
                && CMRepositoryVersionUtil.isKeepHostTemplateSupportedViaBlueprint(clouderaManagerRepoDetails)) {
            instantiator.keepHostTemplates(Boolean.TRUE);
        }
        if (Objects.nonNull(clouderaManagerRepoDetails) && CMRepositoryVersionUtil.isEnableKerberosSupportedViaBlueprint(clouderaManagerRepoDetails)
                && templatePreparationObject.getKerberosConfig().isPresent()) {
            instantiator.setEnableKerberos(new ApiConfigureForKerberosArguments());
        }
        for (ApiClusterTemplateService service : cmTemplate.getServices()) {
            List<String> nonBaseRefs = ofNullable(service.getRoleConfigGroups()).orElse(new ArrayList<>())
                    .stream()
                    .filter(rcg -> rcg.getBase() == null || !rcg.getBase())
                    .map(ApiClusterTemplateRoleConfigGroup::getRefName)
                    .collect(Collectors.toList());
            for (String nonBaseRef : nonBaseRefs) {
                instantiator.addRoleConfigGroupsItem(new ApiClusterTemplateRoleConfigGroupInfo().rcgRefName(nonBaseRef));
            }
        }

        ofNullable(sdxContextName)
                .map(name -> List.of(new ApiDataContextRef().name(name)))
                .map(apiDataContextRefs -> new ApiClusterTemplateClusterSpec().dataContextRefs(apiDataContextRefs))
                .ifPresent(instantiator::clusterSpec);
        cmTemplate.setInstantiator(instantiator);
    }

    public void addVariables(List<ApiClusterTemplateVariable> vars) {
        for (ApiClusterTemplateVariable v : vars) {
            cmTemplate.getInstantiator().addVariablesItem(v);
        }
    }

    public void addServiceConfigs(String serviceType, List<String> roleTypes, List<ApiClusterTemplateConfig> configs) {
        getServiceByType(serviceType).ifPresent(service -> mergeServiceConfigs(service, configs));
    }

    private void mergeServiceConfigs(ApiClusterTemplateService service, List<ApiClusterTemplateConfig> configs) {
        if (ofNullable(service.getServiceConfigs()).orElse(List.of()).isEmpty()) {
            setServiceConfigs(service, configs);
            return;
        }

        Map<String, ApiClusterTemplateConfig> configMap = mapByName(service.getServiceConfigs());
        configs.forEach(config -> configMap.putIfAbsent(config.getName(), config));
        setServiceConfigs(service, configMap.values());
    }

    private Map<String, ApiClusterTemplateConfig> mapByName(Collection<ApiClusterTemplateConfig> configs) {
        return configs.stream()
                    .collect(toMap(
                            ApiClusterTemplateConfig::getName,
                            Function.identity()
                    ));
    }

    private void setServiceConfigs(ApiClusterTemplateService service, Collection<ApiClusterTemplateConfig> configs) {
        service.setServiceConfigs(new ArrayList<>(configs));
    }

    private void setRoleConfigs(ApiClusterTemplateRoleConfigGroup rcg, Collection<ApiClusterTemplateConfig> configs) {
        rcg.setConfigs(new ArrayList<>(configs));
    }

    public void addRoleConfigs(String serviceType, Map<String, List<ApiClusterTemplateConfig>> newConfigMap) {
        Optional<ApiClusterTemplateService> serviceOpt = getServiceByType(serviceType);
        if (serviceOpt.isPresent()) {
            ApiClusterTemplateService service = serviceOpt.get();
            newConfigMap.forEach((configRef, newConfigs) -> service.getRoleConfigGroups().stream().filter(rcg -> rcg.getRefName().equals(configRef))
                    .findFirst().ifPresent(group -> mergeRoleConfigs(group, newConfigs)));
        }
    }

    private void mergeRoleConfigs(ApiClusterTemplateRoleConfigGroup configGroup, List<ApiClusterTemplateConfig> newConfigs) {
        if (ofNullable(configGroup.getConfigs()).orElse(List.of()).isEmpty()) {
            setRoleConfigs(configGroup, newConfigs);
            return;
        }

        Map<String, ApiClusterTemplateConfig> configMap = mapByName(configGroup.getConfigs());
        newConfigs.forEach(config -> configMap.putIfAbsent(config.getName(), config));
        setRoleConfigs(configGroup, configMap.values());
    }

    public boolean isRoleTypePresentInService(String serviceType, List<String> roleTypes) {
        return getServiceByType(serviceType).filter(acts -> isAnyRoleTypePresent(acts, roleTypes)).isPresent();
    }

    private boolean isAnyRoleTypePresent(ApiClusterTemplateService apiClusterTemplateService, List<String> roleTypes) {
        return apiClusterTemplateService.getRoleConfigGroups().stream()
                .anyMatch(rcg -> roleTypes.stream().anyMatch(roleType -> roleType.equalsIgnoreCase(rcg.getRoleType())));
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

    public void setDisplayName(String displayName) {
        cmTemplate.setDisplayName(displayName);
    }

    public void setHostTemplates(List<ApiClusterTemplateHostTemplate> hostTemplates) {
        cmTemplate.setHostTemplates(hostTemplates);
    }

    public void setServices(List<ApiClusterTemplateService> services) {
        cmTemplate.setServices(services);
    }

}
