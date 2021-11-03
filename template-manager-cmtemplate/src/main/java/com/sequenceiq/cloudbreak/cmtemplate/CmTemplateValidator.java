package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateService;
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
    public void validateHostGroupScalingRequest(String accountId, Blueprint blueprint, HostGroup hostGroup, Integer adjustment,
            Collection<InstanceGroup> instanceGroups) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintText());
        Versioned blueprintVersion = () -> templateProcessor.getVersion().get();
        Set<String> services = templateProcessor.getComponentsByHostGroup().get(hostGroup.getName());
        validateNodeManagerAdjustment(hostGroup, adjustment, instanceGroups, templateProcessor);
        for (BlackListedDownScaleRole role : BlackListedDownScaleRole.values()) {
            if (services.contains(role.name()) && adjustment < 0) {
                validateRole(accountId, role, blueprintVersion, templateProcessor);
            }
        }
        for (BlackListedUpScaleRole role : BlackListedUpScaleRole.values()) {
            if (services.contains(role.name()) && adjustment > 0) {
                validateRole(accountId, role, blueprintVersion, templateProcessor);
            }
        }
    }

    private void validateNodeManagerAdjustment(HostGroup hostGroup, Integer adjustment, Collection<InstanceGroup> instanceGroups,
            CmTemplateProcessor templateProcessor) {
        Map<String, Set<String>> nodeManagerComponentInGroups = templateProcessor.getComponentsByHostGroup().entrySet().stream()
                .filter(entry -> entry.getValue().contains("NODEMANAGER"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (nodeManagerComponentInGroups.containsKey(hostGroup.getName())) {
            instanceGroups.stream().filter(instanceGroup -> nodeManagerComponentInGroups.containsKey(instanceGroup.getGroupName()))
                    .map(InstanceGroup::getNodeCount).reduce(Integer::sum).ifPresent(nodeManagerCountInGroups -> {
                if (nodeManagerCountInGroups + adjustment <= 0) {
                    throw new BadRequestException("Scaling adjustment is not allowed, based on the template it would eliminate all the instances with " +
                            "NODEMANAGER role which is not supported.");
                }
            });
        }
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
                && !requiredServiceIsPresented(role.getRequiredService(), templateProcessor)
                && versionEnablesScaling) {
            throw new BadRequestException(String.format("'%s' service is not presented on the cluster, and that is required",
                    role.getRequiredService().get()));
        } else {
            LOGGER.info("Account is entitled for {} so scaling is enabled.", role.getEntitledFor());
        }
    }

    public boolean isVersionEnablesScaling(Versioned blueprintVersion, EntitledForServiceScale role) {
        return role.getBlockedUntilCDPVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(blueprintVersion, role.getBlockedUntilCDPVersionAsVersion());
    }

    private boolean requiredServiceIsPresented(Optional<String> requiredService, CmTemplateProcessor templateProcessor) {
        boolean requiredServiceIsPresented = false;
        Optional<ApiClusterTemplateService> serviceByType = templateProcessor.getServiceByType(requiredService.get());
        if (serviceByType.isPresent()) {
            requiredServiceIsPresented = true;
        }
        return requiredServiceIsPresented;
    }
}
