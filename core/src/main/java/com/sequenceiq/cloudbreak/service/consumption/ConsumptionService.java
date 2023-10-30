package com.sequenceiq.cloudbreak.service.consumption;

import static java.lang.String.join;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu.KuduRoles;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;

@Service
public class ConsumptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ConsumptionClientService consumptionClientService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Value("${cb.consumption.enabled:false}")
    private boolean consumptionEnabled;

    public void scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(StackDto stackDto) {
        executeIfDeploymentFlagAndEntitlementAreSatisfied(stackDto, "attached volume consumption collection scheduling",
                this::scheduleAttachedVolumeConsumptionCollectionForStack);
    }

    private void executeIfDeploymentFlagAndEntitlementAreSatisfied(StackDto stackDto, String operation, BiConsumer<StackDto, String> action) {
        String accountId = stackDto.getAccountId();
        if (consumptionEnabled && entitlementService.isCdpSaasEnabled(accountId)) {
            action.accept(stackDto, operation);
        } else {
            LOGGER.info("Skipping {} for stack because {}", operation,
                    consumptionEnabled ? String.format("CDP_SAAS entitlement is missing for account '%s'", accountId) : "it is disabled for the deployment");
        }
    }

    private void scheduleAttachedVolumeConsumptionCollectionForStack(StackDto stackDto, String operation) {
        BiConsumer<StackDto, ConsumptionType> action = (stackDtoAction, consumptionTypeAction) ->
                scheduleConsumptionCollectionForCloudResourceAndType(stackDtoAction, stackDtoAction.getResourceCrn(), consumptionTypeAction);
        executeIfCloudPlatformAndStackEligibilityAreSatisfied(stackDto, operation, action);
    }

    private void executeIfCloudPlatformAndStackEligibilityAreSatisfied(StackDto stackDto, String operation, BiConsumer<StackDto, ConsumptionType> action) {
        String cloudPlatform = stackDto.getCloudPlatform();
        ConsumptionType consumptionType = getAttachedVolumeConsumptionType(cloudPlatform);
        if (consumptionType != null) {
            if (isStackEligibleForScheduling(stackDto, operation)) {
                action.accept(stackDto, consumptionType);
            } else {
                LOGGER.info("Skipping {} for stack because it is not eligible", operation);
            }
        } else {
            LOGGER.info("Skipping {} for stack because it is not supported for the cloud platform '{}'", operation, cloudPlatform);
        }
    }

    private ConsumptionType getAttachedVolumeConsumptionType(String cloudPlatform) {
        return "AWS".equals(cloudPlatform) ? ConsumptionType.EBS : null;
    }

    private boolean isStackEligibleForScheduling(StackDto stackDto, String operation) {
        boolean eligible = false;
        if (stackDto.getType() == StackType.WORKLOAD) {
            Optional<String> blueprintTextOpt = Optional.ofNullable(stackDto.getBlueprint()).map(Blueprint::getBlueprintJsonText);
            if (blueprintTextOpt.isPresent()) {
                CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintTextOpt.get());
                Set<String> hostGroupsWithKuduMaster = cmTemplateProcessor.getHostGroupsWithComponent(KuduRoles.KUDU_MASTER);
                Set<String> hostGroupsWithKuduTserver = cmTemplateProcessor.getHostGroupsWithComponent(KuduRoles.KUDU_TSERVER);
                if (hostGroupsWithKuduMaster.isEmpty() && hostGroupsWithKuduTserver.isEmpty()) {
                    LOGGER.info("Kudu not found, thus stack '{}' is not eligible for {}.", stackDto.getName(), operation);
                } else {
                    eligible = true;
                    LOGGER.info("Kudu found, thus stack '{}' is eligible for {}. Host groups with KUDU_MASTER: '[{}]'. Host groups with KUDU_TSERVER: '[{}]'.",
                            stackDto.getName(), operation, join(",", hostGroupsWithKuduMaster), join(",", hostGroupsWithKuduTserver));
                }
            } else {
                LOGGER.warn("Blueprint text is unavailable for stack '{}', thus {} is not possible.", stackDto.getName(), operation);
            }
        } else {
            LOGGER.info("Stack '{}' is not a Data Hub, thus it is not eligible for {}.", stackDto.getName(), operation);
        }
        return eligible;
    }

    private void scheduleConsumptionCollectionForCloudResourceAndType(StackDto stackDto, String cloudResourceId, ConsumptionType consumptionType) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        CloudResourceConsumptionRequest request = new CloudResourceConsumptionRequest();
        request.setEnvironmentCrn(stackDto.getEnvironmentCrn());
        request.setMonitoredResourceCrn(stackDto.getResourceCrn());
        request.setMonitoredResourceName(stackDto.getName());
        request.setMonitoredResourceType(ResourceType.DATAHUB);
        request.setCloudResourceId(cloudResourceId);
        request.setConsumptionType(consumptionType);
        String accountId = stackDto.getAccountId();
        LOGGER.info("Executing cloud resource consumption collection scheduling: account '{}', user '{}' and request '{}'", accountId, initiatorUserCrn,
                request);
        consumptionClientService.scheduleCloudResourceConsumptionCollection(accountId, request, initiatorUserCrn);
    }

    public void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(StackDto stackDto) {
        executeIfDeploymentFlagAndEntitlementAreSatisfied(stackDto, "attached volume consumption collection unscheduling",
                this::unscheduleAttachedVolumeConsumptionCollectionForStack);
    }

    private void unscheduleAttachedVolumeConsumptionCollectionForStack(StackDto stackDto, String operation) {
        BiConsumer<StackDto, ConsumptionType> action = (stackDtoAction, consumptionTypeAction) ->
                unscheduleConsumptionCollectionForCloudResource(stackDtoAction.getAccountId(), stackDtoAction.getResourceCrn(), stackDtoAction.getResourceCrn());
        executeIfCloudPlatformAndStackEligibilityAreSatisfied(stackDto, operation, action);
    }

    private void unscheduleConsumptionCollectionForCloudResource(String accountId, String monitoredResourceCrn, String cloudResourceId) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        LOGGER.info("Executing cloud resource consumption collection unscheduling: account '{}', user '{}', CDP resource '{}' and cloud resource '{}'",
                accountId, initiatorUserCrn, monitoredResourceCrn, cloudResourceId);
        consumptionClientService.unscheduleCloudResourceConsumptionCollection(accountId, monitoredResourceCrn, cloudResourceId, initiatorUserCrn);
    }

}
