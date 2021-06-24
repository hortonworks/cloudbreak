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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.springframework.util.CollectionUtils;

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
import com.google.common.base.Enums;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.ResizeRecommendation;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceAttributes;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;

public class CmTemplateProcessor implements BlueprintTextProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateProcessor.class);

    private final ApiClusterTemplate cmTemplate;

    public CmTemplateProcessor(@Nonnull String cmTemplateText) {
        try {
            cmTemplate = JsonUtil.readValue(cmTemplateText, ApiClusterTemplate.class);
            transformHostGroupNameToLowerCase();
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse blueprint text.", e);
        }
    }

    private void transformHostGroupNameToLowerCase() {
        if (!CollectionUtils.isEmpty(cmTemplate.getHostTemplates())) {
            cmTemplate.getHostTemplates().forEach(ht -> ht.setRefName(ht.getRefName().toLowerCase()));
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
    public Map<String, Set<String>> getNonGatewayComponentsByHostGroup() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ServiceComponent::getComponent)
                                .filter(i -> !i.equalsIgnoreCase("GATEWAY"))
                                .collect(toSet())
                ));
    }

    private Map<String, Set<ServiceComponent>> getNonGatewayServicesByHostGroup() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .collect(toMap(
                        Entry::getKey,
                        e -> e.getValue().stream()
                                .filter(i -> !i.getComponent().equalsIgnoreCase("GATEWAY"))
                                .collect(toSet())
                ));

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
    public AutoscaleRecommendation recommendAutoscale() {
        Set<String> time = getRecommendationByBlacklist(BlackListedTimeBasedAutoscaleRole.class, true, List.of());
        Set<String> load = getRecommendationByBlacklist(BlackListedLoadBasedAutoscaleRole.class, true, List.of());
        return new AutoscaleRecommendation(time, load);
    }

    private <T extends Enum<T>> Set<String> getRecommendationByBlacklist(Class<T> enumClass, boolean emptyServiceListBlacklisted,
            List<String> entitlements) {
        Map<String, Set<String>> componentsByHostGroup = getNonGatewayComponentsByHostGroup();
        return getRecommendationByBlacklist(enumClass, emptyServiceListBlacklisted, componentsByHostGroup, entitlements);
    }

    private <T extends Enum<T>> Set<String> getRecommendationByBlacklist(Class<T> enumClass, boolean emptyServiceListBlacklisted,
            Map<String, Set<String>> componentsByHostGroup, List<String> entitlements) {
        LOGGER.info("Get recommendation by blacklisted {} with entitlements {}.", enumClass, entitlements);

        Set<String> recos = new HashSet<>();
        for (Entry<String, Set<String>> hostGroupServices : componentsByHostGroup.entrySet()) {
            if (emptyServiceListBlacklisted && hostGroupServices.getValue().isEmpty()) {
                continue;
            }
            boolean foundBlackListItem = false;
            for (String service : hostGroupServices.getValue()) {
                com.google.common.base.Optional<T> enumValue = Enums.getIfPresent(enumClass, service);
                if (enumValue.isPresent()) {
                    if (EntitledForServiceScale.class.isAssignableFrom(enumClass)) {
                        Entitlement entitledFor = ((EntitledForServiceScale) enumValue.get()).getEntitledFor();
                        if (!entitlements.contains(entitledFor.name())) {
                            foundBlackListItem = true;
                            break;
                        }
                    } else {
                        foundBlackListItem = true;
                        break;
                    }
                }
            }
            if (!foundBlackListItem) {
                recos.add(hostGroupServices.getKey());
            }
        }
        return recos;
    }

    @Override
    public ResizeRecommendation recommendResize(List<String> entitlements) {
        Set<String> upRecos = getRecommendationByBlacklist(BlackListedUpScaleRole.class, false, entitlements);
        Set<String> downRecos = getRecommendationByBlacklist(BlackListedDownScaleRole.class, false, entitlements);
        return new ResizeRecommendation(upRecos, downRecos);
    }

    @Override
    public String getStackVersion() {
        return cmTemplate.getCdhVersion();
    }

    @Override
    public Map<String, Map<String, ServiceAttributes>> getHostGroupBasedServiceAttributes() {
        Map<String, Set<String>> componentsByHostGroup = collectComponentsByHostGroupWithYarnNMs();

        // Re-using the current LoadBasedAutoScaling recommendation to determine hostGroups which can
        // be autoscaled and marked as YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE
        Set<String> computeHostGroups = getRecommendationByBlacklist(BlackListedLoadBasedAutoscaleRole.class,
                true, componentsByHostGroup, List.of());

        Map<String, Map<String, ServiceAttributes>> result = new HashMap<>();

        for (String hg : componentsByHostGroup.keySet()) {
            String instanceType = computeHostGroups.contains(hg) ? YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE
                    : YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_WORKER;

            result.put(hg, Collections.singletonMap(YarnRoles.YARN,
                    new ServiceAttributes(ServiceComponent.of(YarnRoles.YARN, YarnRoles.NODEMANAGER),
                            Collections.singletonMap(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE, instanceType))));
        }
        LOGGER.debug("ServiceAttributes: {}", result);
        return result;
    }

    private Map<String, Set<String>> collectComponentsByHostGroupWithYarnNMs() {
        Map<String, Set<ServiceComponent>> hgToNonGwServiceComponents = getNonGatewayServicesByHostGroup();
        Map<String, Set<ServiceComponent>> hgToNonGwServiceComponentsWithYarnNMs = hgToNonGwServiceComponents.entrySet().stream()
                .filter(e -> isYarnNodemanager(e.getValue()))
                .collect(toMap(Entry::getKey, Entry::getValue));
        return hgToNonGwServiceComponentsWithYarnNMs.entrySet()
                .stream().collect(toMap(e -> e.getKey(), e -> collectComponents(e.getValue())));
    }

    private boolean isYarnNodemanager(Set<ServiceComponent> serviceComponents) {
        return serviceComponents.stream().anyMatch(sc -> YarnRoles.YARN.equalsIgnoreCase(sc.getService())
            && YarnRoles.NODEMANAGER.equalsIgnoreCase(sc.getComponent()));
    }

    private Set<String> collectComponents(Set<ServiceComponent> serviceComponentSet) {
        return serviceComponentSet.stream().map(ServiceComponent::getComponent).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<String> getHostTemplateNames() {
        return Optional.ofNullable(cmTemplate.getHostTemplates())
                .orElse(List.of())
                .stream()
                .map(ApiClusterTemplateHostTemplate::getRefName)
                .collect(Collectors.toList());
    }

    public List<ApiClusterTemplateHostTemplate> getHostTemplates() {
        return cmTemplate.getHostTemplates();
    }

    public boolean everyHostTemplateHasRoleConfigGroupsRefNames() {
        for (ApiClusterTemplateHostTemplate hostTemplate : getHostTemplates()) {
            if (hostTemplate.getRoleConfigGroupsRefNames() == null) {
                return false;
            }
        }
        return true;
    }

    public boolean everyServiceHasRoleConfigGroups() {
        for (ApiClusterTemplateService service : cmTemplate.getServices()) {
            if (service.getRoleConfigGroups() == null) {
                return false;
            }
        }
        return true;
    }

    public Set<ServiceComponent> getAllComponents() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(toSet());
    }

    public boolean isInstantiatorPresent() {
        return cmTemplate.getInstantiator() != null;
    }

    public boolean isRepositoriesPresent() {
        return cmTemplate.getRepositories() != null && !cmTemplate.getRepositories().isEmpty();
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
        newConfigs.forEach(config -> chooseApiClusterTemplateConfig(configMap, config));
        setServiceConfigs(service, configMap.values());
    }

    public void mergeCustomServiceConfigs(ApiClusterTemplateService service, List<ApiClusterTemplateConfig> newCustomServiceConfigs) {
        if (newCustomServiceConfigs.isEmpty()) {
            return;
        } else if (service.getServiceConfigs() == null) {
            setServiceConfigs(service, newCustomServiceConfigs);
        } else {
            setServiceConfigs(service, mergeCustomConfigs(service.getServiceConfigs(), newCustomServiceConfigs));
        }
    }

    public void mergeCustomRoleConfigs(ApiClusterTemplateService service, List<ApiClusterTemplateRoleConfigGroup> newCustomRoleConfigGroups) {
        if (newCustomRoleConfigGroups.isEmpty()) {
            return;
        }
        List<ApiClusterTemplateRoleConfigGroup> currentRoleConfigs = service.getRoleConfigGroups();
        newCustomRoleConfigGroups.forEach(newCustomRoleConfigGroup -> {
            Optional<ApiClusterTemplateRoleConfigGroup> configIfExists = currentRoleConfigs.stream()
                    .filter(currentConfig -> currentConfig.getRoleType().equalsIgnoreCase(newCustomRoleConfigGroup.getRoleType()))
                    .findFirst();
            if (configIfExists.isEmpty()) {
                throw new NotFoundException("Role " + newCustomRoleConfigGroup.getRoleType() + " does not exist for service " + service.getServiceType());
            }
            if (configIfExists.get().getConfigs() == null) {
                setRoleConfigs(configIfExists.get(), newCustomRoleConfigGroup.getConfigs());
            } else {
                List<ApiClusterTemplateConfig> mergedConfigs = mergeCustomConfigs(configIfExists.get().getConfigs(), newCustomRoleConfigGroup.getConfigs());
                setRoleConfigs(configIfExists.get(), mergedConfigs);
            }
        });
    }

    public List<ApiClusterTemplateConfig> mergeCustomConfigs(List<ApiClusterTemplateConfig> currentConfigs, List<ApiClusterTemplateConfig> newCustomConfigs) {
        newCustomConfigs.forEach(config -> {
            Optional<ApiClusterTemplateConfig> configIfExists = currentConfigs.stream()
                    .filter(currentConfig -> currentConfig.getName().equalsIgnoreCase(config.getName()))
                    .findFirst();
            if (configIfExists.isPresent()) {
                if (config.getName().endsWith("_safety_valve")) {
                    String currentValue = configIfExists.get().getValue();
                    String valueToBeAppended = config.getValue();
                    config.setValue(currentValue + '\n' + valueToBeAppended);
                }
                currentConfigs.set(currentConfigs.indexOf(configIfExists.get()), config);
            } else {
                currentConfigs.add(config);
            }
        });
        return currentConfigs;
    }

    private void chooseApiClusterTemplateConfig(Map<String, ApiClusterTemplateConfig> existingConfigs, ApiClusterTemplateConfig newConfig) {
        String configName = newConfig.getName();
        ApiClusterTemplateConfig existingApiClusterTemplateConfig = existingConfigs.get(configName);
        if (existingApiClusterTemplateConfig != null) {
            // OPSAPS-54706 Let's honor the safety valve settings in both bp and generated.
            if (configName.endsWith("_safety_valve")) {
                String oldConfigValue = existingApiClusterTemplateConfig.getValue();
                String newConfigValue = newConfig.getValue();

                // By CB-1452 append the bp config at the end of generated config to give precedence to it. Add a newline in between for it to be safe
                // with property file safety valves and command line safety valves.
                newConfig.setValue(newConfigValue + '\n' + oldConfigValue);
            } else {
                // Again by CB-1452 we need to give precedence to the value given in bp.
                return;
            }
        }
        existingConfigs.put(configName, newConfig);
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
        newConfigs.forEach(config -> chooseApiClusterTemplateConfig(configMap, config));
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
