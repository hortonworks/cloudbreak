package com.sequenceiq.cloudbreak.template.processor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.ResizeRecommendation;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.template.model.ServiceAttributes;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;

public interface BlueprintTextProcessor {

    /**
     * Returns a single word which distinguishes the host node's type based on the stack (Ambari/CM/..)
     * @return stack based word
     */
    String getHostGroupPropertyIdentifier();

    Optional<String> getVersion();

    Map<String, Set<String>> getComponentsByHostGroup();

    Map<String, Set<String>> getNonGatewayComponentsByHostGroup();

    ClusterManagerType getClusterManagerType();

    BlueprintTextProcessor replaceConfiguration(String s, String descriptor);

    BlueprintTextProcessor addComponentToHostgroups(String component, Predicate<String> addToHostgroup);

    BlueprintTextProcessor addComponentToHostgroups(String component, Collection<String> addToHostGroups);

    boolean isComponentExistsInHostGroup(String component, String hostGroup);

    BlueprintTextProcessor setSecurityType(String kerberos);

    BlueprintTextProcessor extendBlueprintGlobalConfiguration(SiteConfigurations configs, boolean forced);

    BlueprintTextProcessor extendBlueprintHostGroupConfiguration(HostgroupConfigurations hostgroupConfigurations, boolean b);

    Set<String> getHostGroupsWithComponent(String component);

    String asText();

    Set<String> getComponentsInHostGroup(String name);

    Set<String> getImpalaCoordinatorsInHostGroup(String name);

    Map<String, InstanceCount> getCardinalityByHostGroup();

    GatewayRecommendation recommendGateway();

    AutoscaleRecommendation recommendAutoscale(Versioned version);

    ResizeRecommendation recommendResize(List<String> entitlements, Versioned version);

    /**
     * Retrieves attributes which may need to be configured for a service.
     * e.g. Whether a YARN NM node is a 'compute-only' node or not.
     *
     * @return a map containing hostGroups as key, and a map of serviceName to attributes as the value
     */
    Map<String, Map<String, ServiceAttributes>> getHostGroupBasedServiceAttributes(Versioned version);

    Set<String> getComputeHostGroups(Versioned version);

    String getStackVersion();

    List<String> getHostTemplateNames();

}
