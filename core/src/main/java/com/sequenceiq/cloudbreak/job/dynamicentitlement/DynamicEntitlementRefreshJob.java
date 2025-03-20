package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowService;

@DisallowConcurrentExecution
@Component
public class DynamicEntitlementRefreshJob extends StatusCheckerJob {

    static final String FLOW_CHAIN_ID = "flowChainId";

    static final String ERROR_COUNT = "errorCount";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Inject
    private DynamicEntitlementRefreshJobService jobService;

    @Inject
    private DynamicEntitlementRefreshConfig config;

    @Inject
    private ImageService imageService;

    @Inject
    private FlowService flowService;

    @Override
    protected void executeJob(JobExecutionContext context) throws JobExecutionException {
        StackDto stack = stackDtoService.getById(getLocalIdAsLong());
        if (!config.isDynamicEntitlementEnabled()) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run because feature is disabled.");
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("DynamicEntitlementRefreshJob will be unscheduled, stack state is {}", stack.getStatus());
            jobService.unschedule(context.getJobDetail().getKey());
        } else if (!isRequiredPackagesInstalled(stack)) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run, because required package (cdp-prometheus) is missing, probably its an old image.");
        } else if (!stack.getStatus().isAvailable()) {
            LOGGER.info("DynamicEntitlementRefreshJob store new watched entitlements without triggering related flow, because status is {}", stack.getStatus());
            dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);
        } else if (anyInstanceStopped(stack)) {
            LOGGER.info("DynamicEntitlementRefreshJob store new watched entitlements without triggering related flow, because there are stopped instances.");
            dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);
        } else {
            LOGGER.info("DynamicEntitlementRefreshJob will apply watched entitlement changes for stack: {}.", stack.getResourceCrn());
            try {
                FlowIdentifier flowIdentifier = dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack);
                if (FlowType.NOT_TRIGGERED != flowIdentifier.getType()) {
                    rescheduleIfPreviousFlowChainFailed(stack, context.getJobDetail(), flowIdentifier.getPollableId());
                }
            } catch (FlowsAlreadyRunningException e) {
                LOGGER.info("DynamicEntitlementRefreshJob cannot run because another flow is running: {}", e.getMessage());
            }
        }
    }

    private boolean isRequiredPackagesInstalled(StackDto stack) {
        try {
            Image image = imageService.getImage(stack.getId());
            if (image != null) {
                Map<String, String> packageVersions = image.getPackageVersions();
                if (packageVersions != null) {
                    return packageVersions.containsKey(ImagePackageVersion.CDP_PROMETHEUS.getKey());
                } else {
                    LOGGER.warn("PackageVersions is null in image {}", image.getImageId());
                }
            } else {
                LOGGER.warn("Image not found");
            }
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found: {}", e.getMessage());
        }
        return false;
    }

    private void rescheduleIfPreviousFlowChainFailed(StackDto stack, JobDetail jobDetail, String flowChainId) {
        int errorCountFromJob = getErrorCountFromJob(jobDetail);
        String flowChainIdFromJob = jobDetail.getJobDataMap().getString(FLOW_CHAIN_ID);
        boolean previousFlowChainFailed = flowService.isPreviousFlowFailed(stack.getId(), flowChainIdFromJob);
        int errorCount = calculateNewErrorCount(errorCountFromJob, previousFlowChainFailed);
        addNewParametersToJobDetail(jobDetail, flowChainId, errorCount);
        jobService.reScheduleWithBackoff(stack.getId(), jobDetail, errorCount);
    }

    private int calculateNewErrorCount(int errorCountFromJob, boolean previousOperationFailed) {
        if (previousOperationFailed) {
            return errorCountFromJob + 1;
        }
        return 0;
    }

    private void addNewParametersToJobDetail(JobDetail jobDetail, String operationId, int errorCount) {
        jobDetail.getJobDataMap().put(FLOW_CHAIN_ID, operationId);
        jobDetail.getJobDataMap().putAsString(ERROR_COUNT, errorCount);
    }

    private int getErrorCountFromJob(JobDetail jobDetail) {
        int result = 0;
        String errorCount = jobDetail.getJobDataMap().getString(ERROR_COUNT);
        if (errorCount != null) {
            try {
                result = Integer.parseInt(errorCount);
            } catch (NumberFormatException e) {
                result = 0;
            }
        }
        return result;
    }

    private boolean anyInstanceStopped(StackDto stack) {
        return stack.getInstanceGroupDtos().stream()
                .map(InstanceGroupDto::getInstanceMetadataViews)
                .flatMap(List::stream)
                .map(InstanceMetadataView::getInstanceStatus)
                .anyMatch(instanceStatus -> InstanceStatus.STOPPED.equals(instanceStatus));
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }
}
