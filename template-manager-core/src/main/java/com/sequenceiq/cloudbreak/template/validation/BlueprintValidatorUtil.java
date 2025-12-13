package com.sequenceiq.cloudbreak.template.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.template.utils.HostGroupUtils;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class BlueprintValidatorUtil {

    @Inject
    private HostGroupUtils hostGroupUtils;

    public void validateHostGroupsMatch(Set<HostGroup> hostGroupsInRequest, Set<String> hostGroupsInBlueprint) {
        Set<String> hostGroupNamesInRequest = hostGroupsInRequest.stream()
                .map(HostGroup::getName)
                .filter(hostGroupUtils::isNotEcsHostGroup)
                .collect(Collectors.toSet());
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

    public void validateInstanceGroups(Collection<HostGroup> hostGroups, Collection<InstanceGroupView> instanceGroups) {
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

    public void validateHostGroupCardinality(Set<HostGroup> hostGroups, Map<String, InstanceCount> blueprintHostGroupCardinality) {
        List<String> failures = new LinkedList<>();
        for (HostGroup hostGroup : hostGroups) {
            InstanceCount requiredCount = blueprintHostGroupCardinality.get(hostGroup.getName());
            int count = hostGroup.getInstanceGroup().getNodeCount();
            if (requiredCount != null && !requiredCount.isAcceptable(count)) {
                failures.add(String.format("(group: '%s', required: %s, actual: %d)", hostGroup.getName(), requiredCount, count));
            }
        }
        if (!failures.isEmpty()) {
            throw new BlueprintValidationException("Node count for the following host groups does not meet requirements: "
                    + String.join(", ", failures));
        }
    }

    public void validateHostGroupNodeCounts(Collection<HostGroup> hostGroups, Map<String, Map.Entry<Predicate<Integer>, String>> hostGroupRestrictions) {
        for (HostGroup hostGroup : hostGroups) {
            if (hostGroupRestrictions.containsKey(hostGroup.getName())) {
                Map.Entry<Predicate<Integer>, String> restrictionEntry = hostGroupRestrictions.get(hostGroup.getName());
                if (!restrictionEntry.getKey().test(hostGroup.getInstanceGroup().getNodeCount())) {
                    throw new BlueprintValidationException(restrictionEntry.getValue());
                }
            }
        }
    }
}
