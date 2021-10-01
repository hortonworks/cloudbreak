package com.sequenceiq.cloudbreak.job.raz;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;


import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.quartz.raz.job.RazSyncerJob;
import com.sequenceiq.cloudbreak.quartz.raz.service.RazSyncerJobService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class CloudbreakRazSyncerJob extends RazSyncerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakRazSyncerJob.class);

    private static final String RAZ_SERVICE = "RANGER_RAZ";

    @Inject
    private RazSyncerJobService jobService;

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private FlowLogService flowLogService;

    public CloudbreakRazSyncerJob(Tracer tracer) {
        super(tracer, "Raz syncer job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackViewService.findById(Long.valueOf(getLocalId())).orElseGet(StackView::new);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        if (flowLogService.isOtherFlowRunning(Long.valueOf(getLocalId()))) {
            LOGGER.debug("Raz syncer job cannot run, because flow is running for stack: {}", getLocalId());
            return;
        }
        try {
            measure(() -> {
                Stack stack = stackService.get(Long.valueOf(getLocalId()));
                Status stackStatus = stack.getStatus();
                if (unschedulableStates().contains(stackStatus)) {
                    LOGGER.debug("Raz syncer check will be unscheduled, stack status is {}", stackStatus);
                    jobService.unschedule();
                } else if (syncableStates().contains(stackStatus)) {
                    ThreadBasedUserCrnProvider.doAsInternalActor(() -> handleStackRazSync(stack));
                } else {
                    LOGGER.debug("Skipping Raz syncer check, stack state is {}", stackStatus);
                }
            }, LOGGER, "Raz syncer check took {} ms for stack {}", getLocalId());
        } catch (Exception e) {
            LOGGER.info("Error during Raz syncer check.", e);
        }
    }

    private void handleStackRazSync(Stack stack) {
        LOGGER.debug("Raz Syncer job is running for Stack: {}", getLocalId());
        ClusterApi clusterApi = apiConnectors.getConnector(stack);
        if (StackType.DATALAKE.equals(stack.getType()) && isClusterManagerRunning(stack, clusterApi)) {
            Cluster cluster = stack.getCluster();
            if (!cluster.isRangerRazEnabled() && clusterApi.clusterModificationService().isServicePresent(cluster.getName(), RAZ_SERVICE)) {
                validateRazEnablement(stack);
                sdxEndpoint.enableRangerRazByCrn(stack.getResourceCrn());
                cluster.setRangerRazEnabled(true);
                clusterService.save(cluster);
            }
        }
    }

    private void validateRazEnablement(Stack stack) {
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        boolean razEntitlementEnabled = entitlementService.razEnabled(Crn.safeFromString(environment.getCreator()).getAccountId());
        if (!razEntitlementEnabled) {
            validationBuilder.error("Provisioning Ranger Raz is not enabled for this account.");
        }
        CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, environment.getCloudPlatform());
        if (!(AWS.equals(cloudPlatform) || AZURE.equals(cloudPlatform))) {
            validationBuilder.error("Provisioning Ranger Raz is only valid for Amazon Web Services and Microsoft Azure.");
        }
        if (!isRazSupported(stack.getStackVersion(), cloudPlatform)) {
            String errorMsg = AWS.equals(cloudPlatform) ? "Provisioning Ranger Raz on Amazon Web Services is only valid for CM version >= 7.2.2 and not " :
                    "Provisioning Ranger Raz on Microsoft Azure is only valid for CM version >= 7.2.1 and not ";
            validationBuilder.error(errorMsg + stack.getStackVersion());
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isRazSupported(String runtime, CloudPlatform cloudPlatform) {
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> AWS.equals(cloudPlatform) ? "7.2.2" : "7.2.1") > -1;
    }

    private boolean isClusterManagerRunning(Stack stack, ClusterApi connector) {
        return !stack.isStopped() && !stack.isStackInDeletionOrFailedPhase() && isCMRunning(connector);
    }

    private boolean isCMRunning(ClusterApi connector) {
        return connector.clusterStatusService().isClusterManagerRunningQuickCheck();
    }

    private boolean unschedulable() {
        return getRemoteResourceCrn() == null;
    }

    private Set<Status> unschedulableStates() {
        return EnumSet.of(
                Status.CREATE_FAILED,
                Status.PRE_DELETE_IN_PROGRESS,
                Status.DELETE_IN_PROGRESS,
                Status.DELETE_FAILED,
                Status.DELETE_COMPLETED,
                Status.DELETED_ON_PROVIDER_SIDE,
                Status.EXTERNAL_DATABASE_CREATION_FAILED,
                Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                Status.EXTERNAL_DATABASE_DELETION_FINISHED,
                Status.EXTERNAL_DATABASE_DELETION_FAILED,
                Status.LOAD_BALANCER_UPDATE_FINISHED,
                Status.LOAD_BALANCER_UPDATE_FAILED
        );
    }

    private Set<Status> syncableStates() {
        return EnumSet.of(
                Status.AVAILABLE,
                Status.AMBIGUOUS,
                Status.MAINTENANCE_MODE_ENABLED,
                Status.UPDATE_REQUESTED,
                Status.UPDATE_IN_PROGRESS,
                Status.UPDATE_FAILED,
                Status.BACKUP_IN_PROGRESS,
                Status.BACKUP_FINISHED
        );
    }
}