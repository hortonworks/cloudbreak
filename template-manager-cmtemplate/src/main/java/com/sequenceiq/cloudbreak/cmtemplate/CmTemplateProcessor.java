package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceCount.atLeast;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceCount.exactly;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isTagsResourceSupportedViaBlueprint;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.cloudera.api.swagger.model.ApiEntityTag;
import com.cloudera.api.swagger.model.ApiProductVersion;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;

public class CmTemplateProcessor implements BlueprintTextProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateProcessor.class);

    private static final String SERVICES_NODE = "services";

    private static final String ROLE_CONFIG_GROUPS = "roleConfigGroups";

    private static final String ROLE_TYPE = "roleType";

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
        return getComponentsInHostGroup(hostGroup).contains(component);
    }

    @Override
    public BlueprintTextProcessor addComponentToHostgroups(String component, Collection<String> addToHostGroups) {
        throw new NotImplementedException("");
    }

    @Override
    public Set<String> getHostGroupsWithComponent(String component) {
        return getComponentsByHostGroup().entrySet().stream()
                .filter(e -> e.getValue().contains(component))
                .map(Map.Entry::getKey)
                .collect(toSet());
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
    public Set<String> getComponentsInHostGroup(String name) {
        return getComponentsByHostGroup().getOrDefault(name, Set.of());
    }

    @Override
    public Set<String> getImpalaCoordinatorsInHostGroup(String name) {
        return getImpalaCoordinatorsByHostGroup().getOrDefault(name, Set.of());
    }

    @Override
    public BlueprintTextProcessor extendBlueprintHostGroupConfiguration(HostgroupConfigurations hostgroupConfigurations, boolean b) {
        throw new NotImplementedException("");
    }

    @Override
    public Map<String, InstanceCount> getCardinalityByHostGroup() {
        Map<String, InstanceCount> result = new TreeMap<>();
        for (ApiClusterTemplateHostTemplate group : Optional.ofNullable(cmTemplate.getHostTemplates()).orElse(List.of())) {
            InstanceCount recommendedCount = recommendInstanceCount(group.getRefName(), group.getCardinality())
                    .orElse(InstanceCount.fallbackInstanceCountRecommendation(group.getRefName()));
            result.put(group.getRefName(), recommendedCount);
        }
        return result;
    }

    /**
     * Crude instance count recommendation based on cardinality.
     */
    private Optional<InstanceCount> recommendInstanceCount(String hostGroup, BigDecimal cardinality) {
        return Optional.ofNullable(cardinality).map(BigDecimal::intValue).map(count ->
                hostGroup.startsWith("master") || hostGroup.startsWith("gateway") ? exactly(count) : atLeast(count)
        );
    }

    public Map<String, Set<ServiceComponent>> getServiceComponentsByHostGroup() {
        Map<String, ServiceComponent> rolesByRoleRef = mapRoleRefsToServiceComponents();
        return collectServiceComponentsByHostGroup(rolesByRoleRef);
    }

    private Map<String, Set<String>> getImpalaCoordinatorsByHostGroup() {
        Map<String, ServiceComponent> rolesByRoleRef = getImpalaCoordinators();
        return collectServiceComponentsByHostGroup(rolesByRoleRef).entrySet().stream()
                .collect(toMap(
                        Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ServiceComponent::getComponent)
                                .collect(toSet())));
    }

    private Map<String, Set<ServiceComponent>> collectServiceComponentsByHostGroup(Map<String, ServiceComponent> rolesByRoleRef) {
        Map<String, Set<ServiceComponent>> result = new HashMap<>();
        List<ApiClusterTemplateHostTemplate> hostTemplates = Optional.ofNullable(cmTemplate.getHostTemplates()).orElse(List.of());
        for (ApiClusterTemplateHostTemplate apiClusterTemplateHostTemplate : hostTemplates) {
            Set<ServiceComponent> components = apiClusterTemplateHostTemplate.getRoleConfigGroupsRefNames().stream()
                    .map(rolesByRoleRef::get)
                    .filter(Objects::nonNull)
                    .collect(toSet());
            result.put(apiClusterTemplateHostTemplate.getRefName(), components);
        }
        return result;
    }

    @Override
    public String getHostGroupPropertyIdentifier() {
        return "template";
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(cmTemplate.getCdhVersion());
    }

    public Optional<String> getCmVersion() {
        return Optional.ofNullable(cmTemplate.getCmVersion());
    }

    @Override
    public Map<String, Set<String>> getComponentsByHostGroup() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ServiceComponent::getComponent)
                                .collect(toSet())
                ));
    }

    @Override
    public GatewayRecommendation recommendGateway() {
        Map<String, InstanceCount> instanceCounts = getCardinalityByHostGroup();

        for (String group : List.of("gateway", "master")) {
            InstanceCount count = instanceCounts.get(group);
            if (InstanceCount.EXACTLY_ONE.equals(count)) {
                return new GatewayRecommendation(Set.of(group));
            }
        }

        return instanceCounts.entrySet().stream()
                .filter(e -> e.getValue().getMinimumCount() <= 1)
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .map(hostGroup -> new GatewayRecommendation(Set.of(hostGroup)))
                .orElseGet(() -> new GatewayRecommendation(Set.of()));
    }

    @Override
    public String getStackVersion() {
        return cmTemplate.getCdhVersion();
    }

    @Override
    public List<String> getHostTemplateNames() {
        return Optional.ofNullable(cmTemplate.getHostTemplates())
                .orElse(List.of())
                .stream()
                .map(ApiClusterTemplateHostTemplate::getRefName)
                .collect(Collectors.toList());
    }

    public Set<ServiceComponent> getAllComponents() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(toSet());
    }

    public void addInstantiator(ClouderaManagerRepo clouderaManagerRepoDetails, TemplatePreparationObject templatePreparationObject, String sdxContextName) {
        ApiClusterTemplateInstantiator instantiator = ofNullable(cmTemplate.getInstantiator()).orElseGet(ApiClusterTemplateInstantiator::new);
        if (instantiator.getClusterName() == null) {
            instantiator.setClusterName(templatePreparationObject.getGeneralClusterConfigs().getClusterName());
        }
        addCmVersionDependantConfigs(clouderaManagerRepoDetails, templatePreparationObject, instantiator);
        for (ApiClusterTemplateService service : ofNullable(cmTemplate.getServices()).orElse(List.of())) {
            List<String> nonBaseRefs = ofNullable(service.getRoleConfigGroups()).orElse(List.of())
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

    private void addCmVersionDependantConfigs(ClouderaManagerRepo cmRepoDetails, TemplatePreparationObject templatePreparationObject,
            ApiClusterTemplateInstantiator instantiator) {
        if (Objects.nonNull(cmRepoDetails)
                && CMRepositoryVersionUtil.isKeepHostTemplateSupportedViaBlueprint(cmRepoDetails)) {
            instantiator.keepHostTemplates(Boolean.TRUE);
        }
        if (Objects.nonNull(cmRepoDetails)
                && CMRepositoryVersionUtil.isIgnorePropertyValidationSupportedViaBlueprint(cmRepoDetails)) {
            instantiator.setLenient(Boolean.TRUE);
        }
        if (Objects.nonNull(cmRepoDetails) && CMRepositoryVersionUtil.isEnableKerberosSupportedViaBlueprint(cmRepoDetails)
                && templatePreparationObject.getKerberosConfig().isPresent()) {
            instantiator.setEnableKerberos(new ApiConfigureForKerberosArguments());
        }
    }

    public void addVariables(List<ApiClusterTemplateVariable> vars) {
        for (ApiClusterTemplateVariable v : vars) {
            cmTemplate.getInstantiator().addVariablesItem(v);
        }
    }

    public void extendTemplateWithAdditionalServices(Map<String, ApiClusterTemplateService> hostGroupServices) {
        for (Entry<String, ApiClusterTemplateService> hostGroupService : hostGroupServices.entrySet()) {
            ApiClusterTemplateService service = hostGroupService.getValue();
            List<String> serviceRefNames = service.getRoleConfigGroups().stream()
                    .map(ApiClusterTemplateRoleConfigGroup::getRefName).collect(Collectors.toList());
            if (getServiceByType(service.getServiceType()).isEmpty()) {
                cmTemplate.addServicesItem(service);
            }
            cmTemplate.getHostTemplates().stream()
                    .filter(hostTemplate -> hostTemplate.getRefName().equals(hostGroupService.getKey()))
                    .forEach(ht -> ht.getRoleConfigGroupsRefNames().addAll(serviceRefNames));
        }
    }

    public void addServiceConfigs(String serviceType, List<String> roleTypes, List<ApiClusterTemplateConfig> configs) {
        getServiceByType(serviceType).ifPresent(service -> mergeServiceConfigs(service, configs));
    }

    public void mergeServiceConfigs(ApiClusterTemplateService service, List<ApiClusterTemplateConfig> newConfigs) {
        if (newConfigs.isEmpty()) {
            return;
        }

        if (ofNullable(service.getServiceConfigs()).orElse(List.of()).isEmpty()) {
            setServiceConfigs(service, newConfigs);
            return;
        }

        Map<String, ApiClusterTemplateConfig> configMap = mapByName(service.getServiceConfigs());
        newConfigs.forEach(config -> configMap.putIfAbsent(config.getName(), config));
        setServiceConfigs(service, configMap.values());
    }

    public Map<String, ApiClusterTemplateConfig> mapByName(Collection<ApiClusterTemplateConfig> configs) {
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

    public void mergeRoleConfigs(ApiClusterTemplateRoleConfigGroup configGroup, List<ApiClusterTemplateConfig> newConfigs) {
        if (newConfigs.isEmpty()) {
            return;
        }

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

    public Optional<ApiClusterTemplateService> getServiceByType(String serviceType) {
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

    public void addDiagnosticTags(TemplatePreparationObject templatePreparationObject, ClouderaManagerRepo clouderaManagerRepo) {
        if (Objects.nonNull(clouderaManagerRepo) && isTagsResourceSupportedViaBlueprint(clouderaManagerRepo)) {
            cmTemplate.addTagsItem(new ApiEntityTag().name("_cldr_cb_origin").value("cloudbreak"));
            cmTemplate.addTagsItem(new ApiEntityTag().name("_cldr_cb_clustertype").value(getClusterType(templatePreparationObject.getStackType())));
        }
    }

    private String getClusterType(StackType stackType) {
        String clusterType;
        switch (stackType) {
            case WORKLOAD:
                clusterType = "Data Hub";
                break;
            case DATALAKE:
                clusterType = "SDX";
                break;
            default:
                clusterType = "Unknown";
        }
        LOGGER.debug("Cluster type tag set to {}", clusterType);
        return clusterType;
    }

    @VisibleForTesting
    Map<String, ServiceComponent> mapRoleRefsToServiceComponents() {
        return ofNullable(cmTemplate.getServices()).orElse(List.of()).stream()
                .filter(service -> service.getRoleConfigGroups() != null)
                .flatMap(service -> service.getRoleConfigGroups().stream()
                        .map(rcg -> Pair.of(service.getServiceType(), rcg)))
                .collect(toMap(
                        pair -> pair.getRight().getRefName(),
                        pair -> ServiceComponent.of(pair.getLeft(), pair.getRight().getRoleType())));
    }

    Map<String, ServiceComponent> getImpalaCoordinators() {
        return ofNullable(cmTemplate.getServices()).orElse(List.of()).stream()
                .filter(service -> service.getRoleConfigGroups() != null)
                .flatMap(service -> service.getRoleConfigGroups().stream()
                        .filter(filterNonCoordinatorImpalaRole())
                        .map(rcg -> Pair.of(service.getServiceType(), rcg)))
                .collect(toMap(
                        pair -> pair.getRight().getRefName(),
                        pair -> ServiceComponent.of(pair.getLeft(), pair.getRight().getRoleType())));
    }

    private Predicate<ApiClusterTemplateRoleConfigGroup> filterNonCoordinatorImpalaRole() {
        return roleConfigGroup -> roleConfigGroup.getRoleType().equals("IMPALAD") &&
                roleConfigGroup.getConfigs().stream().anyMatch(config -> config.getValue().equals("COORDINATOR_ONLY"));
    }

    public void removeDanglingVariableReferences() {
        if (cmTemplate.getServices() != null) {
            Set<String> existingVariables = cmTemplate.getInstantiator() != null && cmTemplate.getInstantiator().getVariables() != null
                    ? cmTemplate.getInstantiator().getVariables().stream()
                    .map(ApiClusterTemplateVariable::getName)
                    .collect(toSet())
                    : Set.of();

            for (ApiClusterTemplateService s : cmTemplate.getServices()) {
                s.setServiceConfigs(removeDanglingVariableReferences(s.getServiceConfigs(), existingVariables, s.getRefName()));
                if (s.getRoleConfigGroups() != null) {
                    for (ApiClusterTemplateRoleConfigGroup rcg : s.getRoleConfigGroups()) {
                        rcg.setConfigs(removeDanglingVariableReferences(rcg.getConfigs(), existingVariables, rcg.getRefName()));
                    }
                }
            }
        }
    }

    private List<ApiClusterTemplateConfig> removeDanglingVariableReferences(
            List<ApiClusterTemplateConfig> configs, Set<String> existingVariables, String refName) {
        if (configs != null) {
            for (Iterator<ApiClusterTemplateConfig> iter = configs.iterator(); iter.hasNext();) {
                ApiClusterTemplateConfig config = iter.next();
                String variable = config.getVariable();
                if (variable != null && !existingVariables.contains(variable)) {
                    LOGGER.info("Removed dangling variable reference '{}': '{}' from section {}", config.getName(), variable, refName);
                    iter.remove();
                }
            }
            if (configs.isEmpty()) {
                return null;
            }
        }
        return configs;
    }

    public boolean isCMComponentExistsInBlueprint(String component) {
        for (Entry<String, Set<String>> entry : getComponentsByHostGroup().entrySet()) {
            for (String entryCompoenent : entry.getValue()) {
                if (component.equalsIgnoreCase(entryCompoenent)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Optional<ApiClusterTemplateConfig> getRoleConfig(String serviceType, String roleType, String configName) {
        return getServiceByType(serviceType).flatMap(
                service -> Optional.ofNullable(service.getRoleConfigGroups()).orElseGet(List::of).stream()
                        .filter(rcg -> Objects.equals(roleType, rcg.getRoleType()))
                        .flatMap(rcg -> Optional.ofNullable(rcg.getConfigs()).orElseGet(List::of).stream())
                        .filter(config -> configName.equals(config.getName()))
                        .findAny());
    }
}
