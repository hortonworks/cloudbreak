package com.sequenceiq.cloudbreak.template.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public class BlueprintValidatorUtil {

    private BlueprintValidatorUtil() { }

    public static void validateHostGroupsMatch(Set<HostGroup> hostGroupsInRequest, Set<String> hostGroupsInBlueprint) {
        Set<String> hostGroupNamesInRequest = hostGroupsInRequest.stream().map(HostGroup::getName).collect(Collectors.toSet());
        Set<String> missingFromRequest = Set.copyOf(Sets.difference(hostGroupsInBlueprint, hostGroupNamesInRequest));
        Set<String> unknownHostGroups = Set.copyOf(Sets.difference(hostGroupNamesInRequest, hostGroupsInBlueprint));
        if (!missingFromRequest.isEmpty()) {
            throw new BlueprintValidationException("The following host groups are missing from the request: "
                    + String.join(", ", missingFromRequest));
        }
        if (!unknownHostGroups.isEmpty()) {
            throw new BlueprintValidationException("Unknown host groups are present in the request: "
                    + String.join(", ", unknownHostGroups));
        }
    }

    public static void validateInstanceGroups(Collection<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups) {
        if (!instanceGroups.isEmpty()) {
            Collection<String> instanceGroupNames = new HashSet<>();
            for (HostGroup hostGroup : hostGroups) {
                String instanceGroupName = hostGroup.getInstanceGroup().getGroupName();
                if (instanceGroupNames.contains(instanceGroupName)) {
                    throw new BlueprintValidationException(String.format(
                            "Instance group '%s' is assigned to more than one hostgroup.", instanceGroupName));
                }
                instanceGroupNames.add(instanceGroupName);
            }
            if (instanceGroups.size() < hostGroups.size()) {
                throw new BlueprintValidationException("Each host group must have an instance group");
            }
        }
    }

    public static Map<String, HostGroup> createHostGroupMap(Iterable<HostGroup> hostGroups) {
        Map<String, HostGroup> groupMap = Maps.newHashMap();
        for (HostGroup hostGroup : hostGroups) {
            groupMap.put(hostGroup.getName(), hostGroup);
        }
        return groupMap;
    }

    public static void validateHostGroupCardinality(Set<HostGroup> hostGroups, Map<String, InstanceCount> blueprintHostGroupCardinality) {
        List<String> failures = new LinkedList<>();
        for (HostGroup hostGroup : hostGroups) {
            InstanceCount requiredCount = blueprintHostGroupCardinality.get(hostGroup.getName());
            int count = hostGroup.getInstanceGroup().getNodeCount();
            if (!requiredCount.isAcceptable(count)) {
                failures.add(String.format("(group: '%s', required: %s, actual: %d)", hostGroup.getName(), requiredCount, count));
            }
        }
        if (!failures.isEmpty()) {
            throw new BlueprintValidationException("Node count for the following host groups does not meet requirements: "
                    + String.join(", ", failures));
        }
    }
}
