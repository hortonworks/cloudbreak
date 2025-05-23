package com.sequenceiq.datalake.service.sdx;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.model.StatusKind;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class ProvisionerService {

    public static final int DELETE_FAILED_RETRY_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionerService.class);

    private static final String CONSTRAINT_VIOLATION_ERROR_MESSAGE_TEMPLATE =
            "Error with resource [STACK], error: [Key (name, workspace_id)=(%s, 1) already exists.]";

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackRequestManifester stackRequestManifester;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    private AttemptResult<StackV4Response> sdxCreationFailed(String statusReason) {
        String errorMessage = "Data Lake creation failed: " + statusReason;
        LOGGER.error(errorMessage);
        return AttemptResults.breakFor(errorMessage);
    }

    public void startStackDeletion(Long id, boolean forced) {
        SdxCluster sdxCluster = sdxService.getById(id);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.deleteInternal(0L, sdxCluster.getClusterName(), forced, initiatorUserCrn));
            sdxStatusService.setStatusForDatalakeAndNotify(
                    DatalakeStatusEnum.STACK_DELETION_IN_PROGRESS, List.of(sdxCluster.getClusterName()),
                    "Datalake stack deletion in progress", sdxCluster
            );
        } catch (NotFoundException e) {
            LOGGER.info("Cannot find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Cannot delete stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Cannot delete cluster, error happened during the operation: " + errorMessage);
        } catch (ProcessingException e) {
            LOGGER.info("Cannot delete stack {} from cloudbreak: {}", sdxCluster.getStackId(), e);
            throw new RuntimeException("Cannot delete cluster, error happened during the operation: " + e.getMessage());
        }
    }

    public void waitCloudbreakClusterDeletion(Long id, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(id);
        AtomicInteger deleteFailedCount = new AtomicInteger(1);
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    LOGGER.info("Deletion polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
                    try {
                        StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                                () -> stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(), sdxCluster.getAccountId()));
                        LOGGER.info("Stack status of SDX {} by response from cloudbreak: {}", sdxCluster.getClusterName(),
                                stackV4Response.getStatus().name());
                        LOGGER.debug("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
                        ClusterV4Response cluster = stackV4Response.getCluster();
                        if (cluster != null) {
                            if (StatusKind.PROGRESS.equals(cluster.getStatus().getStatusKind())) {
                                return AttemptResults.justContinue();
                            }
                            if (Status.DELETE_FAILED.equals(cluster.getStatus())) {
                                // it's a hack, until we implement a nice non-async terminate which can return with flowid..
                                // if it is implemented, please remove this
                                if (deleteFailedCount.getAndIncrement() >= DELETE_FAILED_RETRY_COUNT) {
                                    LOGGER.error("Cluster deletion failed '" + sdxCluster.getClusterName() + "', "
                                            + stackV4Response.getCluster().getStatusReason());
                                    return AttemptResults.breakFor(
                                            "Data Lake deletion failed '" + sdxCluster.getClusterName() + "', "
                                                    + stackV4Response.getCluster().getStatusReason()
                                    );
                                } else {
                                    return AttemptResults.justContinue();
                                }
                            }
                        }
                        if (Status.DELETE_FAILED.equals(stackV4Response.getStatus())) {
                            // it's a hack, until we implement a nice non-async terminate which can return with flowid..
                            // if it is implemented, please remove this
                            if (deleteFailedCount.getAndIncrement() >= DELETE_FAILED_RETRY_COUNT) {
                                LOGGER.error("Stack deletion failed '" + sdxCluster.getClusterName() + "', " + stackV4Response.getStatusReason());
                                return AttemptResults.breakFor(
                                        "Data Lake deletion failed '" + sdxCluster.getClusterName() + "', " + stackV4Response.getStatusReason()
                                );
                            } else {
                                return AttemptResults.justContinue();
                            }
                        } else {
                            return AttemptResults.justContinue();
                        }
                    } catch (NotFoundException e) {
                        return AttemptResults.finishWith(null);
                    }
                });
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, "Datalake stack deleted", sdxCluster);
    }

    public void startStackProvisioning(Long id, DetailedEnvironmentResponse environment) {
        SdxCluster sdxCluster = sdxService.getById(id);
        LOGGER.info("Call cloudbreak with stackrequest");
        try {
            stackRequestManifester.configureStackForSdxCluster(sdxCluster, environment);
            StackV4Request stackV4Request = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class);
            Optional.ofNullable(sdxCluster.getDatabaseCrn()).ifPresent(crn -> {
                stackV4Request.getCluster().setDatabaseServerCrn(crn);
                sdxCluster.setStackRequestToCloudbreak(JsonUtil.writeValueAsStringSilent(stackV4Request));
            });
            StackV4Response stackV4Response;
            try {
                stackV4Response = getStackByCrn(sdxCluster);
            } catch (NotFoundException e) {
                LOGGER.info("Stack does not exist on cloudbreak side, POST new cluster: {}", sdxCluster.getClusterName(), e);
                stackV4Response = postNewClusterAndHandleIfStackAlreadyExists(stackV4Request, sdxCluster);
            }
            sdxCluster.setStackId(stackV4Response.getId());
            sdxCluster.setStackCrn(stackV4Response.getCrn());
            sdxClusterRepository.save(sdxCluster);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, stackV4Response.getFlowIdentifier());
            LOGGER.info("Sdx cluster updated");
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Cannot start provisioning: {}", errorMessage, e);
            throw new RuntimeException("Cannot start provisioning, error happened during the operation: " + errorMessage);
        } catch (IOException e) {
            LOGGER.info("Cannot parse stackrequest to json", e);
            throw new RuntimeException("Cannot write stackrequest to json: " + e.getMessage());
        }
    }

    private StackV4Response getStackByCrn(SdxCluster sdxCluster) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.getByCrn(0L, sdxCluster.getCrn(), null));
    }

    private StackV4Response postNewClusterAndHandleIfStackAlreadyExists(StackV4Request stackV4Request, SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.postInternal(0L, stackV4Request, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            if (errorMessage != null && errorMessage.contains(String.format(CONSTRAINT_VIOLATION_ERROR_MESSAGE_TEMPLATE, sdxCluster.getName()))) {
                LOGGER.warn("The stack already exists despite pre-checks, probably due to a retried POST request. Fetching stack and proceed.");
                return getStackByCrn(sdxCluster);
            } else {
                LOGGER.info("Cannot start provisioning: {}", errorMessage, e);
                throw new RuntimeException("Cannot start provisioning, error happened during the operation: " + errorMessage);
            }
        }
    }

    public StackV4Response waitCloudbreakClusterCreation(Long id, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(id);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
        cloudbreakPoller.pollCreateUntilAvailable(sdxCluster, pollingConfig);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_FINISHED, "Stack created for Datalake", sdxCluster);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(), sdxCluster.getAccountId()));
    }

}
