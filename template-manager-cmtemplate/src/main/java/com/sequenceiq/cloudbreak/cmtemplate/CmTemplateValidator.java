package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidatorUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class CmTemplateValidator implements BlueprintValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmTemplateValidator.class);

    private static final Map<String, Integer> REQUIRED_ROLE_COUNT = Map.ofEntries(
            Map.entry("DATANODE", 1),
            Map.entry("NODEMANAGER", 1),
            Map.entry("HBASE_REGIONSERVER", 1),
            Map.entry("PHOENIX_QUERY_SERVER", 1),
            Map.entry("KUDU_TSERVER", 1),
            Map.entry("KAFKA_BROKER", 3));

    @Inject
    private CmTemplateProcessorFactory processorFactory;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void validate(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroupView> instanceGroups,
            boolean validateServiceCardinality) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintJsonText());
        Map<String, InstanceCount> blueprintHostGroupCardinality = templateProcessor.getCardinalityByHostGroup();

        BlueprintValidatorUtil.validateHostGroupsMatch(hostGroups, blueprintHostGroupCardinality.keySet());
        BlueprintValidatorUtil.validateInstanceGroups(hostGroups, instanceGroups);
        if (validateServiceCardinality) {
            BlueprintValidatorUtil.validateHostGroupCardinality(hostGroups, blueprintHostGroupCardinality);
        }
    }

    @Override
    public void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, Optional<ClouderaManagerProduct> cdhProduct,
            String hostGroupName, Integer adjustment,
            Collection<InstanceGroup> instanceGroups) {
        validateHostGroupScalingRequest(accountId, blueprint, Map.of(hostGroupName, adjustment), cdhProduct, instanceGroups);
    }

    public void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, Map<String, Integer> instanceGroupAdjustments,
            Optional<ClouderaManagerProduct> cdhProduct, Collection<InstanceGroup> instanceGroups) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintJsonText());
        validateRequiredRoleCountInCluster(instanceGroupAdjustments, instanceGroups, templateProcessor);
        instanceGroupAdjustments.forEach((hostGroupName, adjustment) -> {
            validateBlackListedScalingRoles(accountId, templateProcessor, hostGroupName, adjustment, cdhProduct);
        });
    }

    public boolean isVersionEnablesScaling(Versioned blueprintVersion, BlackListedScaleRole role) {
        return role.getBlockedUntilCDPVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(blueprintVersion, role.getBlockedUntilCDPVersionAsVersion());
    }

    protected void validateBlackListedScalingRoles(String accountId, CmTemplateProcessor templateProcessor, String hostGroupName,
            Integer adjustment, Optional<ClouderaManagerProduct> cdhProduct) {
        Set<String> services = templateProcessor.getComponentsByHostGroup().get(hostGroupName);
        if (adjustment < 0) {
            for (BlackListedDownScaleRole role : BlackListedDownScaleRole.values()) {
                if (services.contains(role.name())) {
                    validateRole(accountId, role, cdhProduct, templateProcessor);
                }
            }
        }
        if (adjustment > 0) {
            for (BlackListedUpScaleRole role : BlackListedUpScaleRole.values()) {
                if (services.contains(role.name())) {
                    validateRole(accountId, role, cdhProduct, templateProcessor);
                }
            }
        }
    }

    private void validateRole(String accountId, BlackListedScaleRole role, Optional<ClouderaManagerProduct> cdhProduct,
        CmTemplateProcessor templateProcessor) {
        Versioned blueprintVersion = () -> cdhProduct.isEmpty() ? "7.0.0" : cdhProduct.get().getVersion();
        boolean versionEnablesScaling = isVersionEnablesScaling(blueprintVersion, role);
        boolean entitledFor = role.getEntitledFor().isEmpty() ? false : entitlementService.isEntitledFor(accountId, role.getEntitledFor().get());
        if (entitlementAndVersionRestrictionsAreEmpty(role)) {
            throw new BadRequestException(String.format("'%s' service is not enabled to scaling " + role.scaleType(), role.name()));
        } else if (notSupportedVersionForScaling(role, versionEnablesScaling, entitledFor)) {
            throw new BadRequestException(String.format("'%s' service is not enabled to scaling " + role.scaleType() + " until CDP %s",
                    role.name(), role.getBlockedUntilCDPVersion().get()));
        } else if (notEntitledForScaling(role, entitledFor)) {
            throw new BadRequestException(String.format("'%s' service is not enabled to scaling" + role.scaleType(), role.name()));
        } else if (requiredServiceNotPresented(role, templateProcessor, versionEnablesScaling)) {
            throw new BadRequestException(String.format("'%s' service is not presented on the cluster, and that is required",
                    role.getRequiredService().get()));
        } else {
            LOGGER.info("Account is entitled for {} so scaling " + role.scaleType() + " is enabled.", role.getEntitledFor());
        }
    }

    private boolean requiredServiceNotPresented(BlackListedScaleRole role, CmTemplateProcessor templateProcessor, boolean versionEnablesScaling) {
        return role.getRequiredService().isPresent()
                && !isRequiredServicePresent(role.getRequiredService(), templateProcessor)
                && versionEnablesScaling;
    }

    private boolean notEntitledForScaling(BlackListedScaleRole role, boolean entitledFor) {
        return role.getBlockedUntilCDPVersion().isEmpty() && !entitledFor;
    }

    private boolean notSupportedVersionForScaling(BlackListedScaleRole role, boolean versionEnablesScaling, boolean entitledFor) {
        return role.getBlockedUntilCDPVersion().isPresent() && !versionEnablesScaling && !entitledFor;
    }

    private boolean entitlementAndVersionRestrictionsAreEmpty(BlackListedScaleRole role) {
        return role.getBlockedUntilCDPVersion().isEmpty() && role.getEntitledFor().isEmpty();
    }

    private boolean isRequiredServicePresent(Optional<String> requiredService, CmTemplateProcessor templateProcessor) {
        return templateProcessor.getServiceByType(requiredService.get()).isPresent();
    }

    private void validateRequiredRoleCountInCluster(Map<String, Integer> instanceGroupAdjustments, Collection<InstanceGroup> instanceGroups,
            CmTemplateProcessor templateProcessor) {
        if (CollectionUtils.isNotEmpty(instanceGroups)) {
            Set<String> targetGroups = instanceGroupAdjustments.keySet();
            LOGGER.debug("Host group adjustments: {}", instanceGroupAdjustments);
            REQUIRED_ROLE_COUNT.forEach((role, requiredCount) -> {
                Set<String> groupsWithRoles = findGroupsWithRoles(role, templateProcessor);
                if (hasCommonElement(targetGroups, groupsWithRoles)) {
                    List<InstanceGroup> filteredInstanceGroups = instanceGroups.stream()
                            .filter(instanceGroup -> groupsWithRoles.contains(instanceGroup.getGroupName()))
                            .collect(Collectors.toList());
                    String groupNames = filteredInstanceGroups.stream().map(InstanceGroup::getGroupName).collect(Collectors.joining(", "));
                    LOGGER.debug("Host group(s) with {} role: {}", role, groupNames);
                    filteredInstanceGroups.stream()
                            .map(instanceGroup -> getAdjustedNodeCount(instanceGroupAdjustments, role, instanceGroup))
                            .reduce(Integer::sum)
                            .ifPresent(remainingInstances -> validateRemainingInstanceCount(role, requiredCount, groupNames, remainingInstances));
                }
            });
        }
    }

    private Set<String> findGroupsWithRoles(String role, CmTemplateProcessor templateProcessor) {
        return templateProcessor.getComponentsByHostGroup()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(role))
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    private boolean hasCommonElement(Set<String> setA, Set<String> setB) {
        return !Collections.disjoint(setA, setB);
    }

    private int getAdjustedNodeCount(Map<String, Integer> instanceGroupAdjustments, String role, InstanceGroup instanceGroup) {
        if (instanceGroupAdjustments.containsKey(instanceGroup.getGroupName())) {
            int adjustedNodeCount = instanceGroup.getNodeCount() +
                    instanceGroupAdjustments.get(instanceGroup.getGroupName());
            LOGGER.debug("{} host group with {} role has {} node(s). After the adjustment it will have {} node(s).",
                    instanceGroup.getGroupName(), role, instanceGroup.getNodeCount(), adjustedNodeCount);
            return adjustedNodeCount;
        } else {
            LOGGER.debug("{} host group with {} role has {} node(s).",
                    instanceGroup.getGroupName(), role, instanceGroup.getNodeCount());
            return instanceGroup.getNodeCount();
        }
    }

    private void validateRemainingInstanceCount(String role, Integer requiredCount, String groupNames, Integer remainingInstances) {
        LOGGER.debug("{} remaining instances, required: {} in groups of {} for {} role",
                remainingInstances, requiredCount, groupNames, role);
        if (remainingInstances < requiredCount) {
            throw new BadRequestException(String.format(
                    "Scaling adjustment is not allowed. %s role must be present on %s host(s) but after the scaling operation %s " +
                            "host(s) would have this role. Based on the template this role is present on the %s " +
                            "host group(s).", role, requiredCount, remainingInstances, groupNames));
        }
    }
}
