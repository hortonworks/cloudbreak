package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

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
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.ClientErrorExceptionHandler;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;

@Service
public class ProvisionerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionerService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackRequestManifester stackRequestManifester;

    @Inject
    private SdxNotificationService notificationService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public void startStackDeletion(Long id) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            try {
                stackV4Endpoint.delete(0L, sdxCluster.getClusterName(), false);
                sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.STACK_DELETION_IN_PROGRESS, "Datalake deletion in progress", sdxCluster);
                sdxClusterRepository.save(sdxCluster);
                notificationService.send(ResourceEvent.SDX_CLUSTER_DELETION_STARTED, sdxCluster);
            } catch (NotFoundException e) {
                LOGGER.info("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
            } catch (ClientErrorException e) {
                String errorMessage = ClientErrorExceptionHandler.getErrorMessage(e);
                LOGGER.info("Can not delete stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
                throw new RuntimeException("Cannot delete stack, client error happened on Cloudbreak side: " + errorMessage);
            } catch (ProcessingException e) {
                LOGGER.info("Can not delete stack {} from cloudbreak: {}", sdxCluster.getStackId(), e);
                throw new RuntimeException("Cannot delete stack, client error happened on Cloudbreak side: " + e.getMessage());
            }
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    public void waitCloudbreakClusterDeletion(Long id, PollingConfig pollingConfig, String requestId) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        LOGGER.info("Deletion polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        try {
                            MDCBuilder.addRequestId(requestId);
                            StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
                            LOGGER.info("Stack status of SDX {} by response from cloudbreak: {}", sdxCluster.getClusterName(),
                                    stackV4Response.getStatus().name());
                            LOGGER.debug("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
                            if (Status.DELETE_FAILED.equals(stackV4Response.getStatus())) {
                                notificationService.send(ResourceEvent.SDX_CLUSTER_DELETION_FAILED, sdxCluster);
                                return AttemptResults.breakFor(
                                        "Stack deletion failed '" + sdxCluster.getClusterName() + "', " + stackV4Response.getStatusReason()
                                );
                            } else {
                                return AttemptResults.justContinue();
                            }
                        } catch (NotFoundException e) {
                            return AttemptResults.finishWith(null);
                        }
                    });
            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.STACK_DELETED, "Datalake deleted", sdxCluster);
            sdxClusterRepository.save(sdxCluster);
            notificationService.send(ResourceEvent.SDX_CLUSTER_DELETION_FINISHED, sdxCluster);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    public void startStackProvisioning(Long id, DetailedEnvironmentResponse environment, DatabaseServerStatusV4Response database) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            stackRequestManifester.configureStackForSdxCluster(sdxCluster, environment);
            LOGGER.info("Call cloudbreak with stackrequest");
            try {
                StackV4Request stackV4Request = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class);
                Optional.ofNullable(database).map(DatabaseServerStatusV4Response::getResourceCrn).ifPresent(crn -> {
                    stackV4Request.getCluster().setDatabaseServerCrn(crn);
                    sdxCluster.setStackRequestToCloudbreak(JsonUtil.writeValueAsStringSilent(stackV4Request));
                });
                StackV4Response stackV4Response = stackV4Endpoint.post(0L, stackV4Request);
                sdxCluster.setStackId(stackV4Response.getId());
                sdxCluster.setStackCrn(stackV4Response.getCrn());
                sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
                sdxClusterRepository.save(sdxCluster);
                notificationService.send(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, sdxCluster);
                LOGGER.info("Sdx cluster updated");
            } catch (ClientErrorException e) {
                String errorMessage = ClientErrorExceptionHandler.getErrorMessage(e);
                LOGGER.info("Can not start provisioning: {}", errorMessage, e);
                throw new RuntimeException("Can not start provisioning, client error happened on Cloudbreak side: " + errorMessage);
            } catch (IOException e) {
                LOGGER.info("Can not parse stackrequest to json", e);
                throw new RuntimeException("Can not write stackrequest to json: " + e.getMessage());
            }
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    public void waitCloudbreakClusterCreation(Long id, PollingConfig pollingConfig, String requestId) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        LOGGER.info("Polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        try {
                            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                                LOGGER.info("Cloudbreak stack polling cancelled in inmemory store, id: " + sdxCluster.getId());
                                return AttemptResults.breakFor("Cloudbreak stack polling cancelled in inmemory store, id: " + sdxCluster.getId());
                            }
                            MDCBuilder.addRequestId(requestId);
                            StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
                            LOGGER.info("Stack status of SDX {} by response from cloudbreak: {}", sdxCluster.getClusterName(),
                                    stackV4Response.getStatus().name());
                            LOGGER.debug("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
                            ClusterV4Response cluster = stackV4Response.getCluster();
                            if (stackAndClusterAvailable(stackV4Response, cluster)) {
                                return AttemptResults.finishWith(stackV4Response);
                            } else {
                                if (Status.CREATE_FAILED.equals(stackV4Response.getStatus())) {
                                    LOGGER.info("Stack creation failed {}", stackV4Response.getName());
                                    return sdxCreationFailed(sdxCluster, stackV4Response.getStatusReason());
                                } else if (Status.CREATE_FAILED.equals(stackV4Response.getCluster().getStatus())) {
                                    LOGGER.info("Cluster creation failed {}", stackV4Response.getCluster().getName());
                                    return sdxCreationFailed(sdxCluster, stackV4Response.getCluster().getStatusReason());
                                } else {
                                    return AttemptResults.justContinue();
                                }
                            }
                        } catch (NotFoundException e) {
                            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
                            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
                        }
                    });
            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.RUNNING, "Datalake is running", sdxCluster);
            sdxClusterRepository.save(sdxCluster);
            notificationService.send(ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED, sdxCluster);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    private AttemptResult<StackV4Response> sdxCreationFailed(SdxCluster sdxCluster, String statusReason) {
        LOGGER.info("SDX creation failed, statusReason: " + statusReason);
        notificationService.send(ResourceEvent.SDX_CLUSTER_CREATION_FAILED, sdxCluster);
        return AttemptResults.breakFor("SDX creation failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }
}
