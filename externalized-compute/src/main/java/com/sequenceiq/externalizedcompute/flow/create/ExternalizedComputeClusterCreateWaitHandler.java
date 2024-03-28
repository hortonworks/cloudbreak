package com.sequenceiq.externalizedcompute.flow.create;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.DescribeClusterResponse;
import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@Component
public class ExternalizedComputeClusterCreateWaitHandler extends ExceptionCatcherEventHandler<ExternalizedComputeClusterCreateWaitRequest> {

    public static final String CREATE_FAILED_STATUS = "CREATE_FAILED";

    public static final String RUNNING_STATUS = "RUNNING";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateWaitHandler.class);

    @Value("${externalizedcompute.create.wait.sleep.time.sec:10}")
    private int sleepTime;

    @Value("${externalizedcompute.create.wait.sleep.time.limit.hour:2}")
    private int timeLimit;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private LiftieGrpcClient liftieGrpcClient;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExternalizedComputeClusterCreateWaitRequest> event) {
        return new ExternalizedComputeClusterCreateFailedEvent(resourceId, event.getData().getActorCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExternalizedComputeClusterCreateWaitRequest> event) {
        Long resourceId = event.getData().getResourceId();
        ExternalizedComputeCluster externalizedComputeCluster =
                externalizedComputeClusterService.getExternalizedComputeCluster(resourceId);
        String actorCrn = event.getData().getActorCrn();
        return Polling.stopAfterDelay(timeLimit, TimeUnit.HOURS)
                .stopIfException(false)
                .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                .run(() -> {
                    DescribeClusterResponse cluster =
                            liftieGrpcClient.describeCluster(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster), actorCrn);
                    LOGGER.debug("Cluster response: {}", cluster);
                    switch (cluster.getStatus()) {
                        case RUNNING_STATUS -> {
                            LOGGER.debug("Cluster created successfully");
                            return AttemptResults.finishWith(
                                    new ExternalizedComputeClusterCreateWaitSuccessResponse(resourceId, actorCrn));
                        }
                        case CREATE_FAILED_STATUS -> {
                            LOGGER.error("Cluster status is a failed status: {}", cluster.getStatus());
                            String errorMessage = cluster.getMessage();
                            return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, actorCrn,
                                    new RuntimeException("Cluster creation failed. Status: " + cluster.getStatus() + ". Message: " + errorMessage)));
                        }
                        default -> {
                            LOGGER.info("Neither \"RUNNING\" status nor \"CREATE_FAILED\" status: {}", cluster.getStatus());
                            return AttemptResults.justContinue();
                        }
                    }
                });
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterCreateWaitRequest.class);
    }
}
