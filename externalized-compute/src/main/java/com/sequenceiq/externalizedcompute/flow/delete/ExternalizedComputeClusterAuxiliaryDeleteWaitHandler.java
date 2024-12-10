package com.sequenceiq.externalizedcompute.flow.delete;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClusterItem;
import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@Component
public class ExternalizedComputeClusterAuxiliaryDeleteWaitHandler extends ExceptionCatcherEventHandler<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> {

    public static final String DELETED_STATUS = "DELETED";

    public static final String DELETE_FAILED_STATUS = "DELETE_FAILED";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterAuxiliaryDeleteWaitHandler.class);

    @Value("${externalizedcompute.delete.aux.wait.sleep.time.ms:10000}")
    private int sleepTime;

    @Value("${externalizedcompute.delete.aux.wait.sleep.time.limit.seconds:7200}")
    private int timeLimit;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private LiftieGrpcClient liftieGrpcClient;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> event) {
        return new ExternalizedComputeClusterDeleteFailedEvent(resourceId, event.getData().getActorCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExternalizedComputeClusterAuxiliaryDeleteWaitRequest> event) {
        Long resourceId = event.getData().getResourceId();
        ExternalizedComputeCluster externalizedComputeCluster =
                externalizedComputeClusterService.getExternalizedComputeCluster(resourceId);
        String environmentCrn = externalizedComputeCluster.getEnvironmentCrn();
        String actorCrn = event.getData().getActorCrn();
        boolean force = event.getData().isForce();
        boolean preserveCluster = event.getData().isPreserveCluster();
        try {
            return Polling.stopAfterDelay(timeLimit, TimeUnit.SECONDS)
                    .stopIfException(false)
                    .waitPeriodly(sleepTime, TimeUnit.MILLISECONDS)
                    .run(() -> {
                        List<ListClusterItem> auxClusters = liftieGrpcClient.listAuxClusters(environmentCrn, actorCrn);
                        LOGGER.info("Auxiliary clusters from liftie: {}", auxClusters);
                        for (ListClusterItem listClusterItem : auxClusters) {
                            LOGGER.debug("Cluster status for {}: {}", listClusterItem.getClusterName(), listClusterItem.getStatus());
                            switch (listClusterItem.getStatus()) {
                                case DELETED_STATUS -> {
                                    LOGGER.debug("Cluster deleted successfully");
                                    auxClusters.remove(listClusterItem);
                                }
                                case DELETE_FAILED_STATUS -> LOGGER.error("{} auxiliary cluster status is a failed status: {}, message: {}",
                                        listClusterItem.getClusterName(), listClusterItem.getStatus(), listClusterItem.getMessage());
                                default -> LOGGER.info("Neither \"DELETED\" status nor \"DELETE_FAILED\" status for {}: {}",
                                        listClusterItem.getClusterName(), listClusterItem.getStatus());
                            }
                        }
                        if (auxClusters.isEmpty()) {
                            return AttemptResults.finishWith(
                                    new ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse(resourceId, actorCrn, force, preserveCluster));
                        }
                        boolean onlyFailedRemaining = auxClusters.stream().map(ListClusterItem::getStatus).allMatch(DELETE_FAILED_STATUS::equals);
                        if (onlyFailedRemaining) {
                            if (force) {
                                LOGGER.warn("Although some of the auxiliary cluster delete failed, we proceeded with the deletion since the force flag was " +
                                                "enabled");
                                return AttemptResults.finishWith(
                                        new ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse(resourceId, actorCrn, force, preserveCluster));
                            } else {
                                String errorMessage = "Auxiliary cluster deletion failed:\n" + auxClusters.stream().map(listClusterItem ->
                                                listClusterItem.getClusterName() + " -> " + listClusterItem.getStatus() + " - Message: " +
                                                        listClusterItem.getMessage()).collect(Collectors.joining("\n"));
                                return AttemptResults.finishWith(new ExternalizedComputeClusterDeleteFailedEvent(resourceId, actorCrn,
                                        new RuntimeException(errorMessage)));
                            }
                        }
                        return AttemptResults.justContinue();
                    });
        } catch (PollerStoppedException e) {
            if (force) {
                LOGGER.warn("Although the auxiliary cluster delete timed out, we proceeded with the deletion since the force flag was enabled");
                return new ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse(resourceId, actorCrn, force, preserveCluster);
            } else {
                LOGGER.warn("Auxiliary cluster deletion timed out for environment: {}.", externalizedComputeCluster.getEnvironmentCrn(), e);
                return new ExternalizedComputeClusterDeleteFailedEvent(resourceId, actorCrn, new RuntimeException("Auxiliary cluster deletion timed out for " +
                        "environment: " + environmentCrn));
            }
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterAuxiliaryDeleteWaitRequest.class);
    }
}
