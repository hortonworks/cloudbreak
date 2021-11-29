package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidatorUtil;

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
    public void validate(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups,
            boolean validateServiceCardinality) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintText());
        Map<String, InstanceCount> blueprintHostGroupCardinality = templateProcessor.getCardinalityByHostGroup();

        BlueprintValidatorUtil.validateHostGroupsMatch(hostGroups, blueprintHostGroupCardinality.keySet());
        BlueprintValidatorUtil.validateInstanceGroups(hostGroups, instanceGroups);
        if (validateServiceCardinality) {
            BlueprintValidatorUtil.validateHostGroupCardinality(hostGroups, blueprintHostGroupCardinality);
        }
    }

    @Override
    public void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, String hostGroupName, Integer adjustment,
            Collection<InstanceGroup> instanceGroups) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintText());
        validateRequiredRoleCountInCluster(hostGroupName, adjustment, instanceGroups, templateProcessor);
        validateBlackListedScalingRoles(accountId, templateProcessor, hostGroupName, adjustment);
    }

    public void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, Map<String, Integer> instanceGroupAdjustments,
            Collection<InstanceGroup> instanceGroups) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintText());
        validateRequiredRoleCountInCluster(instanceGroupAdjustments, instanceGroups, templateProcessor);
        instanceGroupAdjustments.forEach((hostGroupName, adjustment) -> {
            validateBlackListedScalingRoles(accountId, templateProcessor, hostGroupName, adjustment);
        });
    }

    public boolean isVersionEnablesScaling(Versioned blueprintVersion, EntitledForServiceScale role) {
        return role.getBlockedUntilCDPVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(blueprintVersion, role.getBlockedUntilCDPVersionAsVersion());
    }

    private void validateBlackListedScalingRoles(String accountId, CmTemplateProcessor templateProcessor, String hostGroupName, Integer adjustment) {
        Versioned blueprintVersion = () -> templateProcessor.getVersion().get();
        Set<String> services = templateProcessor.getComponentsByHostGroup().get(hostGroupName);
        if (adjustment < 0) {
            for (BlackListedDownScaleRole role : BlackListedDownScaleRole.values()) {
                if (services.contains(role.name())) {
                    validateRole(accountId, role, blueprintVersion, templateProcessor);
                }
            }
        }
        if (adjustment > 0) {
            for (BlackListedUpScaleRole role : BlackListedUpScaleRole.values()) {
                if (services.contains(role.name())) {
                    validateRole(accountId, role, blueprintVersion, templateProcessor);
                }
            }
        }
    }

    private void validateRequiredRoleCountInCluster(
            String hostGroupName,
            Integer adjustment,
            Collection<InstanceGroup> instanceGroups,
            CmTemplateProcessor templateProcessor) {
        REQUIRED_ROLE_COUNT.forEach((role, requiredCount) -> {
            Set<String> instanceGroupsWithRoles = templateProcessor.getComponentsByHostGroup()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().contains(role))
                    .map(Entry::getKey)
                    .collect(Collectors.toSet());
            if (instanceGroupsWithRoles.contains(hostGroupName)) {
                instanceGroups.stream()
                        .filter(instanceGroup -> instanceGroupsWithRoles.contains(instanceGroup.getGroupName()))
                        .map(InstanceGroup::getNodeCount)
                        .reduce(Integer::sum)
                        .ifPresent(roleCountInGroups -> {
                            if (roleCountInGroups + adjustment < requiredCount) {
                                throw new BadRequestException(String.format(
                                        "Scaling adjustment is not allowed, based on the template it would eliminate all the instances with "
                                                + "%s role which is not supported.", role));
                            }
                        });
            }
        });
    }

    private void validateRequiredRoleCountInCluster(
            Map<String, Integer> instanceGroupAdjustments,
            Collection<InstanceGroup> instanceGroups,
            CmTemplateProcessor templateProcessor) {
        REQUIRED_ROLE_COUNT.forEach((role, requiredCount) -> {
            Set<String> instanceGroupsWithRoles = templateProcessor.getComponentsByHostGroup()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().contains(role))
                    .map(Entry::getKey)
                    .collect(Collectors.toSet());
            instanceGroups.stream()
                    .filter(instanceGroup -> instanceGroupsWithRoles.contains(instanceGroup.getGroupName()))
                    .map(instanceGroup ->
                            instanceGroup.getNodeCount() + instanceGroupAdjustments.getOrDefault(instanceGroup.getGroupName(), 0))
                    .reduce(Integer::sum)
                    .ifPresent(remainingInstances -> {
                        if (remainingInstances < requiredCount) {
                            throw new BadRequestException(String.format(
                                    "Scaling adjustment is not allowed, based on the template it would eliminate all the instances with "
                                            + "%s role which is not supported.", role));
                        }
                    });
        });
    }

    private void validateRole(String accountId, EntitledForServiceScale role, Versioned blueprintVersion, CmTemplateProcessor templateProcessor) {
        boolean versionEnablesScaling = isVersionEnablesScaling(blueprintVersion, role);
        boolean entitledFor = entitlementService.isEntitledFor(accountId, role.getEntitledFor());
        if (role.getBlockedUntilCDPVersion().isPresent() && !versionEnablesScaling && !entitledFor) {
            throw new BadRequestException(String.format("'%s' service is not enabled to scale until CDP %s",
                    role.name(), role.getBlockedUntilCDPVersion().get()));
        } else if (role.getBlockedUntilCDPVersion().isEmpty() && !entitledFor) {
            throw new BadRequestException(String.format("'%s' service is not enabled to scale",
                    role.name()));
        } else if (role.getRequiredService().isPresent()
                && !isRequiredServicePresent(role.getRequiredService(), templateProcessor)
                && versionEnablesScaling) {
            throw new BadRequestException(String.format("'%s' service is not presented on the cluster, and that is required",
                    role.getRequiredService().get()));
        } else {
            LOGGER.info("Account is entitled for {} so scaling is enabled.", role.getEntitledFor());
        }
    }

    private boolean isRequiredServicePresent(Optional<String> requiredService, CmTemplateProcessor templateProcessor) {
        return templateProcessor.getServiceByType(requiredService.get()).isPresent();
    }
}
