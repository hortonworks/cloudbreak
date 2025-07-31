package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceCount.atLeast;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceCount.exactly;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isTagsResourceSupportedViaBlueprint;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

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
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.ResizeRecommendation;
import com.sequenceiq.cloudbreak.cluster.model.ClusterHostAttributes;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFile;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFileFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplateEndpoint;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateRoleConfig;
import com.sequenceiq.cloudbreak.template.TemplateServiceConfig;
import com.sequenceiq.cloudbreak.template.model.ServiceAttributes;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationPropertyView;

public class CmTemplateProcessor implements BlueprintTextProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateProcessor.class);

    private static final String ROLE_CONFIG_VALUE = "COORDINATOR_ONLY";

    private static final String ROLE_CONFIG_NAME = "impalad_specialization";

    private static final String ROLE_IMPALAD = "IMPALAD";

    private static final String SERVICE_TYPE = "IMPALA";

    private static final Set<String> INI_FILE_SAFETY_VALVE_CONFIGS = Set.of(HueConfigProvider.HUE_SERVICE_SAFETY_VALVE,
            HueConfigProvider.HUE_SERVER_HUE_SAFETY_VALVE);

    private static final IniFileFactory DEFAULT_INI_FILE_FACTORY = new IniFileFactory();

    private final ApiClusterTemplate cmTemplate;

    private final IniFileFactory iniFileFactory;

    public CmTemplateProcessor(@Nonnull String cmTemplateText) {
        // Not using the Spring bean because it would add a superfluous StaticApplicationContext.getApplicationContext() dependency
        this(cmTemplateText, DEFAULT_INI_FILE_FACTORY);
    }

    @VisibleForTesting
    CmTemplateProcessor(@Nonnull String cmTemplateText, IniFileFactory iniFileFactory) {
        try {
            cmTemplate = JsonUtil.readValue(cmTemplateText, ApiClusterTemplate.class);
            transformHostGroupNameToLowerCase();
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse blueprint text.", e);
        }
        this.iniFileFactory = iniFileFactory;
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
                                .collect(toSet()),
                        (a1, a2) -> a1));
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
                                .collect(toSet()),
                        (a1, a2) -> a1
                ));
    }

    private Map<String, Set<ServiceComponent>> getNonGatewayServicesByHostGroup() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .collect(toMap(
                        Entry::getKey,
                        e -> e.getValue().stream()
                                .filter(i -> !i.getComponent().equalsIgnoreCase("GATEWAY"))
                                .collect(toSet()),
                        (a1, a2) -> a1
                ));

    }

    @Override
    public Map<String, Set<String>> getComponentsByHostGroup() {
        return getServiceComponentsByHostGroup().entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(ServiceComponent::getComponent)
                                .collect(toSet()),
                        (a1, a2) -> a1
                ));
    }

    @Override
    public GatewayRecommendation recommendGateway() {
        GatewayRecommendation explicitGatewayRecommendation = getExplicitGatewayRecommendation();
        if (explicitGatewayRecommendation != null) {
            return explicitGatewayRecommendation;
        }

        Map<String, InstanceCount> instanceCounts = getCardinalityByHostGroup();

        for (String group : List.of("gateway", "master")) {
            InstanceCount count = instanceCounts.get(group);
            if (InstanceCount.EXACTLY_ONE.equals(count)) {
                LOGGER.debug("Returning host group '{}' as the Knox Gateway recommendation based on name & cardinality.", group);
                return new GatewayRecommendation(Set.of(group));
            }
        }

        return instanceCounts.entrySet().stream()
                .filter(e -> e.getValue().getMinimumCount() <= 1)
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .map(hostGroup -> {
                    LOGGER.debug("Returning host group '{}' as the Knox Gateway recommendation based on lexicographic order & minimum count.", hostGroup);
                    return new GatewayRecommendation(Set.of(hostGroup));
                })
                .orElseGet(() -> {
                    LOGGER.warn("Cannot determine Knox Gateway recommendation, returning an empty set.");
                    return new GatewayRecommendation(Set.of());
                });
    }

    private GatewayRecommendation getExplicitGatewayRecommendation() {
        LOGGER.debug("Checking if Knox Gateway is explicitly defined in any host groups");
        Set<String> knoxGatewayGroupNames = getHostGroupsWithComponent(KnoxRoles.KNOX_GATEWAY);
        if (knoxGatewayGroupNames.isEmpty()) {
            LOGGER.warn("Knox Gateway is not explicitly defined, continuing with recommendation calculation.");
            return null;
        }

        LOGGER.debug(knoxGatewayGroupNames.size() > 1 ? ("Found Knox Gateway explicitly defined in multiple host groups '{}' that is an ambiguous setup, " +
                "trying to choose among them based on their cardinalities.") :
                "Found Knox Gateway explicitly defined in exactly 1 host group '{}', verifying its cardinality.", knoxGatewayGroupNames);

        // Perform cardinality check even for a single matching host group.
        // In case of multiple matches, this is only a best-effort heuristics; there is no proper unambiguous choice by just looking at blueprint contents.
        String knoxGatewayGroupName = getKnoxGatewayGroupNameWithMinPositiveCardinality(knoxGatewayGroupNames);
        if (knoxGatewayGroupName == null) {
            LOGGER.warn("Cannot find a proper match among the host groups where Knox Gateway is explicitly defined, " +
                    "continuing with recommendation calculation.");
            return null;
        }

        LOGGER.debug(knoxGatewayGroupNames.size() > 1 ? ("Found Knox Gateway explicitly defined in host group '{}' that has " +
                "the smallest positive cardinality, returning it as the recommendation.") :
                "Found Knox Gateway explicitly defined in exactly 1 host group '{}' with a positive cardinality, returning it as the recommendation.",
                knoxGatewayGroupName);
        return new GatewayRecommendation(Set.of(knoxGatewayGroupName));
    }

    private String getKnoxGatewayGroupNameWithMinPositiveCardinality(Set<String> knoxGatewayGroupNames) {
        List<ApiClusterTemplateHostTemplate> knoxGatewayGroups = Optional.ofNullable(cmTemplate.getHostTemplates()).orElse(List.of()).stream()
                .filter(group -> group.getRefName() != null && knoxGatewayGroupNames.contains(group.getRefName()))
                .collect(Collectors.toList());
        ensureAllKnoxGatewayGroupsFound(knoxGatewayGroupNames, knoxGatewayGroups);

        OptionalInt minPositiveCardinalityOpt = knoxGatewayGroups.stream()
                .mapToInt(this::getHostGroupCardinality)
                .filter(cardinality -> cardinality > 0)
                .min();
        if (minPositiveCardinalityOpt.isEmpty()) {
            LOGGER.warn("None of the host groups '{}' where Knox Gateway is explicitly defined has a positive cardinality.", knoxGatewayGroupNames);
            return null;
        }

        int minPositiveCardinality = minPositiveCardinalityOpt.getAsInt();
        LOGGER.debug("Filtering host groups '{}' where Knox Gateway is explicitly defined by the smallest positive cardinality {}.", knoxGatewayGroupNames,
                minPositiveCardinality);
        return knoxGatewayGroups.stream()
                .filter(group -> getHostGroupCardinality(group) == minPositiveCardinality)
                .map(ApiClusterTemplateHostTemplate::getRefName)
                .sorted()
                .findFirst()
                .orElse(null);
    }

    private void ensureAllKnoxGatewayGroupsFound(Set<String> knoxGatewayGroupNamesExpected, List<ApiClusterTemplateHostTemplate> knoxGatewayGroups) {
        if (knoxGatewayGroupNamesExpected.size() != knoxGatewayGroups.size()) {
            Set<String> knoxGatewayGroupNamesFound = knoxGatewayGroups.stream()
                    .map(ApiClusterTemplateHostTemplate::getRefName)
                    .collect(toSet());
            Set<String> knoxGatewayGroupNamesMissing = new HashSet<>(knoxGatewayGroupNamesExpected);
            knoxGatewayGroupNamesMissing.removeAll(knoxGatewayGroupNamesFound);
            LOGGER.warn("Cannot find host groups '{}' where Knox Gateway is explicitly defined.", knoxGatewayGroupNamesMissing);
        }
    }

    private int getHostGroupCardinality(ApiClusterTemplateHostTemplate group) {
        return Optional.ofNullable(group.getCardinality())
                .map(BigDecimal::intValue)
                .orElse(Integer.MIN_VALUE);
    }

    private Set<String> getRecommendationForTimeBasedScaling(Set<String> timeBasedScalingRecommendationByBlacklist,
            Map<String, Set<String>> componentsByHostGroup, List<String> entitlements) {
        Set<String> recommendedHostGroups = new HashSet<>();
        if (entitlements.contains(Entitlement.DATAHUB_IMPALA_SCHEDULE_BASED_SCALING.name())) {
            Map<String, List<String>> roleConfigGroupsByHostGroup = getRoleConfigGroupsByHostGroup();
            for (String hostGroup : timeBasedScalingRecommendationByBlacklist) {
                List<String> hostGroupRoleConfigNames = roleConfigGroupsByHostGroup.get(hostGroup);
                if (!(isImpalaCoordinatorRole(ROLE_CONFIG_VALUE, ROLE_CONFIG_NAME, SERVICE_TYPE, hostGroupRoleConfigNames))) {
                    recommendedHostGroups.add(hostGroup);
                }
            }
        } else {
            for (String hostGroup : timeBasedScalingRecommendationByBlacklist) {
                if (!componentsByHostGroup.get(hostGroup).contains(ROLE_IMPALAD)) {
                    recommendedHostGroups.add(hostGroup);
                }
            }
        }
        return recommendedHostGroups;
    }

    private Map<String, List<String>> getRoleConfigGroupsByHostGroup() {
        Map<String, List<String>> roleConfigGroupsByHostGroup = new HashMap<>();
        for (ApiClusterTemplateHostTemplate hostTemplate : getHostTemplates()) {
            roleConfigGroupsByHostGroup.put(hostTemplate.getRefName(), hostTemplate.getRoleConfigGroupsRefNames());
        }
        return roleConfigGroupsByHostGroup;
    }

    private boolean isImpalaCoordinatorRole(String roleConfigValue, String roleConfigName, String serviceType, List<String> roleConfigGroups) {
        for (ApiClusterTemplateService apiClusterTemplateService : cmTemplate.getServices()) {
            if (apiClusterTemplateService.getServiceType().equalsIgnoreCase(serviceType)) {
                for (ApiClusterTemplateRoleConfigGroup apiClusterTemplateRoleConfigGroup : apiClusterTemplateService.getRoleConfigGroups()) {
                    if (roleConfigGroups.contains(apiClusterTemplateRoleConfigGroup.getRefName()) || CollectionUtils.isEmpty(roleConfigGroups)) {
                        for (ApiClusterTemplateConfig apiClusterTemplateConfig : apiClusterTemplateRoleConfigGroup.getConfigs()) {
                            if (apiClusterTemplateConfig.getValue().equals(roleConfigValue)
                                    && apiClusterTemplateConfig.getName().equals(roleConfigName)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public AutoscaleRecommendation recommendAutoscale(Versioned version, List<String> entitlements) {
        Map<String, Set<String>> componentsByHostGroup = getNonGatewayComponentsByHostGroup();
        Set<String> time = getRecommendationByBlacklist(BlackListedTimeBasedAutoscaleRole.class, true,
                version, List.of(), componentsByHostGroup);
        Set<String> load = getRecommendationByBlacklist(BlackListedLoadBasedAutoscaleRole.class, true,
                version, List.of(), componentsByHostGroup);
        if (!load.isEmpty()) {
            load = getRecommendationForLoadBasedByRequiredRoles(load, componentsByHostGroup);
        }
        if (!time.isEmpty()) {
            time = getRecommendationForTimeBasedScaling(time, componentsByHostGroup, entitlements);
        }
        return new AutoscaleRecommendation(time, load);
    }

    private Set<String> getRecommendationForLoadBasedByRequiredRoles(Set<String> nonBlacklistedLoadBasedHostgroups,
            Map<String, Set<String>> componentsByHostGroup) {
        LOGGER.info("Recommendation for load based scaling based on blacklisted roles: {}", nonBlacklistedLoadBasedHostgroups);

        Set<String> finalLoadBasedHostGroups = new HashSet<>();
        Set<String> requiredRoles = Stream.of(RequiredLoadBasedAutoscaleRole.values())
                .map(RequiredLoadBasedAutoscaleRole::name)
                .collect(toSet());
        for (String loadBasedHostgroup : nonBlacklistedLoadBasedHostgroups) {
            Set<String> rolesPresentInHostgroup = componentsByHostGroup.get(loadBasedHostgroup);
            boolean allRequiredRolesPresent = true;
            for (String requiredRole : requiredRoles) {
                if (!rolesPresentInHostgroup.contains(requiredRole)) {
                    LOGGER.info("Discarding hostgroup because required roles are not present on this hostgroup: '{}'", loadBasedHostgroup);
                    allRequiredRolesPresent = false;
                    break;
                }
            }
            if (allRequiredRolesPresent) {
                finalLoadBasedHostGroups.add(loadBasedHostgroup);
            }
        }
        LOGGER.info("Recommendation for load based scaling based on blacklisted and required roles: {}", finalLoadBasedHostGroups);
        return finalLoadBasedHostGroups;
    }

    private <T extends Enum<T>> Set<String> getRecommendationByBlacklist(Class<T> enumClass, boolean emptyServiceListBlacklisted,
        Versioned version, List<String> entitlements, Map<String, Set<String>> componentsByHostGroup) {
        return getRecommendationByBlacklist(enumClass, emptyServiceListBlacklisted, componentsByHostGroup, version, entitlements);
    }

    private <T extends Enum<T>> Set<String> getRecommendationByBlacklist(Class<T> enumClass, boolean emptyServiceListBlacklisted,
            Map<String, Set<String>> componentsByHostGroup, Versioned version, List<String> entitlements) {
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
                    if (BlackListedScaleRole.class.isAssignableFrom(enumClass)) {
                        BlackListedScaleRole blackListedScaleRole = (BlackListedScaleRole) enumValue.get();
                        Optional<Entitlement> entitledFor = blackListedScaleRole.getEntitledFor();
                        if (isEntitledForTheOptionalEntitlement(entitlements, blackListedScaleRole, entitledFor)
                                && !isVersionEnablesScaling(version, blackListedScaleRole)) {
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

    private boolean isEntitledForTheOptionalEntitlement(List<String> entitlements, BlackListedScaleRole blackListedScaleRole,
        Optional<Entitlement> entitledFor) {
        return (entitledFor.isEmpty() && blackListedScaleRole.getBlockedUntilCDPVersion().isEmpty())
                || (entitledFor.isPresent() && !entitlements.contains(entitledFor.get().name()));
    }

    public boolean isVersionEnablesScaling(Versioned blueprintVersion, BlackListedScaleRole role) {
        return role.getBlockedUntilCDPVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(blueprintVersion, role.getBlockedUntilCDPVersionAsVersion());
    }

    @Override
    public ResizeRecommendation recommendResize(List<String> entitlements, Versioned version) {
        Map<String, Set<String>> componentsByHostGroup = getNonGatewayComponentsByHostGroup();
        Set<String> upRecos = getRecommendationByBlacklist(BlackListedUpScaleRole.class, false,
                version, entitlements, componentsByHostGroup);
        Set<String> downRecos = getRecommendationByBlacklist(BlackListedDownScaleRole.class, false,
                version, entitlements, componentsByHostGroup);
        return new ResizeRecommendation(upRecos, downRecos);
    }

    @Override
    public String getStackVersion() {
        return cmTemplate.getCdhVersion();
    }

    @Override
    public Map<String, Map<String, ServiceAttributes>> getHostGroupBasedServiceAttributes(Versioned version) {
        Map<String, Set<String>> componentsByHostGroup = collectComponentsByHostGroupWithYarnNMs();

        // Re-using the current LoadBasedAutoScaling recommendation to determine hostGroups which can
        // be autoscaled and marked as YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE
        Set<String> computeHostGroups = getRecommendationByBlacklist(BlackListedLoadBasedAutoscaleRole.class,
                true, componentsByHostGroup, version, List.of());

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

    @Override
    public Set<String> getComputeHostGroups(Versioned version) {
        Map<String, Set<String>> componentsByHostGroup = collectComponentsByHostGroupWithYarnNMs();
        // Re-using the current LoadBasedAutoScaling recommendation to determine hostGroups which can be autoscaled
        return getRecommendationByBlacklist(BlackListedLoadBasedAutoscaleRole.class,
                true, componentsByHostGroup, version, List.of());
    }

    private Map<String, Set<String>> collectComponentsByHostGroupWithYarnNMs() {
        Map<String, Set<ServiceComponent>> hgToNonGwServiceComponents = getNonGatewayServicesByHostGroup();
        Map<String, Set<ServiceComponent>> hgToNonGwServiceComponentsWithYarnNMs = hgToNonGwServiceComponents.entrySet().stream()
                .filter(e -> isYarnNodemanager(e.getValue()))
                .collect(toMap(Entry::getKey, Entry::getValue, (a1, a2) -> a1));
        return hgToNonGwServiceComponentsWithYarnNMs.entrySet()
                .stream().collect(toMap(
                        Entry::getKey,
                        e -> collectComponents(e.getValue()),
                        (a1, a2) -> a1));
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

    public void addServiceConfigs(String serviceType, List<ApiClusterTemplateConfig> configs) {
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

    private List<CustomConfigurationPropertyView> getCustomServiceConfigs(Set<CustomConfigurationPropertyView> configs) {
        return configs.stream()
                .filter(config -> config.getRoleType() == null)
                .collect(Collectors.toList());
    }

    public Map<String, List<ApiClusterTemplateConfig>> getCustomServiceConfigsMap(Set<CustomConfigurationPropertyView> configProperties) {
        Map<String, List<ApiClusterTemplateConfig>> serviceConfigsMap = new HashMap<>();
        List<CustomConfigurationPropertyView> serviceConfigs = getCustomServiceConfigs(configProperties);
        serviceConfigs.forEach(serviceConfig -> {
            serviceConfigsMap.computeIfAbsent(serviceConfig.getServiceType(), k -> new ArrayList<>());
            serviceConfigsMap.get(serviceConfig.getServiceType()).add(new ApiClusterTemplateConfig()
                    .name(serviceConfig.getName()).value(serviceConfig.getValue()));
        });
        return serviceConfigsMap;
    }

    private List<CustomConfigurationPropertyView> getCustomRoleConfigs(Set<CustomConfigurationPropertyView> configs) {
        return configs.stream()
                .filter(config -> config.getRoleType() != null)
                .collect(Collectors.toList());
    }

    private Optional<ApiClusterTemplateRoleConfigGroup> filterRCGsByRoleType(List<ApiClusterTemplateRoleConfigGroup> roleConfigs, String roleType) {
        return roleConfigs.stream().filter(rcg -> roleType.equalsIgnoreCase(rcg.getRoleType())).findFirst();
    }

    public Map<String, List<ApiClusterTemplateRoleConfigGroup>> getCustomRoleConfigsMap(Set<CustomConfigurationPropertyView> configProperties) {
        Map<String, List<ApiClusterTemplateRoleConfigGroup>> roleConfigsMap = new HashMap<>();
        List<CustomConfigurationPropertyView> roleConfigGroups = getCustomRoleConfigs(configProperties);
        roleConfigGroups.forEach(roleConfigProperty -> {
            String name = roleConfigProperty.getName();
            String value = roleConfigProperty.getValue();
            String service = roleConfigProperty.getServiceType();
            String role = roleConfigProperty.getRoleType();
            roleConfigsMap.computeIfAbsent(service, k -> new ArrayList<>());
            Optional<ApiClusterTemplateRoleConfigGroup> roleConfigGroup = filterRCGsByRoleType(roleConfigsMap.get(service), role);
            if (roleConfigGroup.isPresent()) {
                roleConfigGroup.get().getConfigs().add(new ApiClusterTemplateConfig().name(name).value(value));
            } else {
                ApiClusterTemplateRoleConfigGroup roleToAdd =
                        new ApiClusterTemplateRoleConfigGroup().roleType(role)
                                .addConfigsItem(new ApiClusterTemplateConfig().name(name).value(value));
                roleConfigsMap.get(service).add(roleToAdd);
            }
        });
        return roleConfigsMap;
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
            List<ApiClusterTemplateRoleConfigGroup> configGroups = currentRoleConfigs.stream()
                    .filter(currentConfig -> currentConfig.getRoleType().equalsIgnoreCase(newCustomRoleConfigGroup.getRoleType()))
                    .collect(Collectors.toList());
            if (configGroups.isEmpty()) {
                throw new NotFoundException("Role " + newCustomRoleConfigGroup.getRoleType() + " does not exist for service " + service.getServiceType());
            }
            for (ApiClusterTemplateRoleConfigGroup configGroup : configGroups) {
                if (configGroup.getConfigs() == null) {
                    setRoleConfigs(configGroup, newCustomRoleConfigGroup.getConfigs());
                } else {
                    List<ApiClusterTemplateConfig> mergedConfigs = mergeCustomConfigs(configGroup.getConfigs(), newCustomRoleConfigGroup.getConfigs());
                    setRoleConfigs(configGroup, mergedConfigs);
                }
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
                    if (INI_FILE_SAFETY_VALVE_CONFIGS.contains(config.getName())) {
                        LOGGER.info("Merging INI file safety valve config values: '{}'", config.getName());
                        IniFile safetyValve = iniFileFactory.create();
                        valueToBeAppended = String.join("\n", Arrays.asList(valueToBeAppended.split("\\\\n")));
                        safetyValve.addContent(currentValue);
                        safetyValve.addContent(valueToBeAppended);
                        config.setValue(safetyValve.print());
                    } else {
                        currentValue = currentValue + '\n' + String.join("\n", Arrays.asList(valueToBeAppended.split("\\\\n")));
                        config.setValue(currentValue);
                    }
                }
                currentConfigs.set(currentConfigs.indexOf(configIfExists.get()), config);
            } else {
                if (config.getName().endsWith("_safety_valve")) {
                    String valueToBeAppended = config.getValue();
                    String currentValue = String.join("\n", Arrays.asList(valueToBeAppended.split("\\\\n")));
                    config.setValue(currentValue);
                }
                currentConfigs.add(config);
            }
        });
        return currentConfigs;
    }

    private void chooseApiClusterTemplateConfig(Map<String, ApiClusterTemplateConfig> existingConfigs, ApiClusterTemplateConfig newConfig) {
        String configName = newConfig.getName();
        ApiClusterTemplateConfig existingApiClusterTemplateConfig = existingConfigs.get(configName);
        if (existingApiClusterTemplateConfig != null) {
            LOGGER.info("Found config present in both blueprint and generated settings: '{}'", configName);
            // OPSAPS-54706 Let's honor the safety valve settings in both bp and generated.
            if (configName.endsWith("_safety_valve")) {
                String oldConfigValue = existingApiClusterTemplateConfig.getValue();
                String newConfigValue = newConfig.getValue();

                if (INI_FILE_SAFETY_VALVE_CONFIGS.contains(configName)) {
                    LOGGER.info("Merging INI file safety valve config values: '{}'", configName);
                    // Merge INI file configs, giving precedence to the blueprint (old) value
                    IniFile safetyValve = iniFileFactory.create();
                    safetyValve.addContent(newConfigValue);
                    safetyValve.addContent(oldConfigValue);
                    newConfig.setValue(safetyValve.print());
                } else {
                    LOGGER.info("Merging regular safety valve config values: '{}'", configName);
                    // By CB-1452 append the bp config at the end of generated config to give precedence to it. Add a newline in between for it to be safe
                    // with property file safety valves and command line safety valves.
                    newConfig.setValue(newConfigValue + '\n' + oldConfigValue);
                }
            } else {
                LOGGER.info("Ignoring generated value and keeping blueprint default for config: '{}'", configName);
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
                        Function.identity(),
                        (a1, a2) -> a1
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

    public boolean isServiceTypePresent(String serviceType) {
        if (Strings.isNullOrEmpty(serviceType)) {
            return false;
        }
        return cmTemplate.getServices().stream().anyMatch(s -> serviceType.equalsIgnoreCase(s.getServiceType()));
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
                attr -> cmTemplate.getInstantiator().addHostsItem(new ApiClusterTemplateHostInfo()
                        .hostName(attr.get(ClusterHostAttributes.FQDN))
                        .hostTemplateRefName(hostGroup)
                        .rackId(Strings.isNullOrEmpty(attr.get(ClusterHostAttributes.RACK_ID)) ? null : attr.get(ClusterHostAttributes.RACK_ID))
                )
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
                        pair -> ServiceComponent.of(pair.getLeft(), pair.getRight().getRoleType()),
                        (a1, a2) -> a1));
    }

    Map<String, ServiceComponent> getImpalaCoordinators() {
        return ofNullable(cmTemplate.getServices()).orElse(List.of()).stream()
                .filter(service -> service.getRoleConfigGroups() != null)
                .flatMap(service -> service.getRoleConfigGroups().stream()
                        .filter(filterNonCoordinatorImpalaRole())
                        .map(rcg -> Pair.of(service.getServiceType(), rcg)))
                .collect(toMap(
                        pair -> pair.getRight().getRefName(),
                        pair -> ServiceComponent.of(pair.getLeft(), pair.getRight().getRoleType()),
                        (a1, a2) -> a1));
    }

    private Predicate<ApiClusterTemplateRoleConfigGroup> filterNonCoordinatorImpalaRole() {
        return roleConfigGroup -> "IMPALAD".equals(roleConfigGroup.getRoleType()) && roleConfigGroup.getConfigs() != null &&
                roleConfigGroup.getConfigs().stream().anyMatch(config -> "COORDINATOR_ONLY".equals(config.getValue()));
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

    public boolean doesCMComponentExistsInBlueprint(String component) {
        for (Entry<String, Set<String>> entry : getComponentsByHostGroup().entrySet()) {
            for (String entryComponent : entry.getValue()) {
                if (component.equalsIgnoreCase(entryComponent)) {
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

    public List<String> getHostTemplateRoleNames(String groupName) {
        return Optional.ofNullable(cmTemplate.getHostTemplates())
                .orElse(List.of())
                .stream()
                .filter(apiTemplate -> apiTemplate.getRefName().equals(groupName))
                .map(ApiClusterTemplateHostTemplate::getRoleConfigGroupsRefNames)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<String> getHostNamesInGroup(String group) {
        return Optional.ofNullable(cmTemplate.getInstantiator())
                .stream()
                .map(ApiClusterTemplateInstantiator::getHosts)
                .flatMap(Collection::stream)
                .filter(host -> Objects.equals(host.getHostTemplateRefName(), group))
                .map(ApiClusterTemplateHostInfo::getHostName)
                .toList();
    }

    public List<String> getHostsWithComponent(String component) {
        return getHostGroupsWithComponent(component).stream()
                .flatMap(hostGroup -> getHostNamesInGroup(hostGroup).stream())
                .toList();
    }

    public boolean isHybridDatahub(TemplatePreparationObject source) {
        return StackType.WORKLOAD.equals(source.getStackType())
                && source.getDatalakeView().isPresent()
                && BlueprintHybridOption.BURST_TO_CLOUD.equals(source.getBlueprintView().getHybridOption());
    }

    public Set<TemplateEndpoint> calculateEndpoints() {
        List<String> nameNodes = getHostsWithComponent(HdfsRoles.NAMENODE);
        String endpoint;
        if (nameNodes.size() > 1) {
            TemplateRoleConfig nameServiceConfig = getTemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "dfs_federation_namenode_nameservice")
                    .orElseThrow(() -> new CloudbreakServiceException("Failed to determine HDFS namenode nameservice"));
            endpoint = nameServiceConfig.value();
        } else {
            endpoint = String.format("hdfs://%s:%s", nameNodes.getFirst(), HdfsConfigHelper.DEFAULT_NAMENODE_PORT);
        }
        return Set.of(new TemplateEndpoint(HdfsRoles.HDFS, HdfsRoles.NAMENODE, endpoint));
    }

    public Set<TemplateServiceConfig> calculateServiceConfigs() {
        return Set.of();
    }

    public Set<TemplateRoleConfig> calculateRoleConfigs() {
        Set<TemplateRoleConfig> roleConfigs = new HashSet<>();
        List<String> nameNodes = getHostsWithComponent(HdfsRoles.NAMENODE);
        if (nameNodes.size() > 1) {
            TemplateRoleConfig nameServiceConfig = getTemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "dfs_federation_namenode_nameservice")
                    .orElseThrow(() -> new CloudbreakServiceException("Failed to determine HDFS namenode nameservice"));
            roleConfigs.add(nameServiceConfig);
            String nameService = nameServiceConfig.value();
            Set<String> nameNodeNames = new HashSet<>();
            for (int i = 0; i < nameNodes.size(); i++) {
                String nameNodeName = "namenode" + (i + 1);
                nameNodeNames.add(nameNodeName);
                String nameNodeNameReference = nameService + '.' + nameNodeName;
                String nameNodeUrl = nameNodes.get(i);
                roleConfigs.add(new TemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE,
                        "dfs.namenode.rpc-address." + nameNodeNameReference, nameNodeUrl + ":" + HdfsConfigHelper.DEFAULT_NAMENODE_PORT));
                roleConfigs.add(new TemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE,
                        "dfs.namenode.servicerpc-address." + nameNodeNameReference, nameNodeUrl + ":8022"));
                roleConfigs.add(new TemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE,
                        "dfs.namenode.http-address." + nameNodeNameReference, nameNodeUrl + ":9870"));
                roleConfigs.add(new TemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE,
                        "dfs.namenode.https-address." + nameNodeNameReference, nameNodeUrl + ":9871"));
            }
            roleConfigs.add(new TemplateRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE,
                    "dfs.ha.namenodes." + nameService, Joiner.on(',').join(nameNodeNames)));
        }
        return roleConfigs;
    }

    private Optional<TemplateRoleConfig> getTemplateRoleConfig(String service, String role, String configKey) {
        return getRoleConfig(service, role, configKey).map(ApiClusterTemplateConfig::getValue)
            .map(configValue -> new TemplateRoleConfig(service, role, configKey, configValue));
    }
}
