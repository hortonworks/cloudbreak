package com.sequenceiq.externalizedcompute.flow.create;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.DefaultApi;
import com.cloudera.model.CommonClusterView;
import com.cloudera.model.GithubInfraClouderaComLiftieLiftiePkgCommonStatusMessage;
import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.externalizedcompute.service.LiftieService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ExternalizedComputeClusterCreateWaitHandler extends ExceptionCatcherEventHandler<ExternalizedComputeClusterCreateWaitRequest> {

    public static final String CREATING_STATUS = "CREATING";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateWaitHandler.class);

    @Value("${externalizedcompute.create.wait.sleep.time.sec:10}")
    private int sleepTime;

    @Value("${externalizedcompute.create.wait.sleep.time.limit.hour:2}")
    private int timeLimit;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private LiftieService liftieService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ExternalizedComputeClusterCreateWaitRequest> event) {
        return new ExternalizedComputeClusterCreateFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ExternalizedComputeClusterCreateWaitRequest> event) {
        Long resourceId = event.getData().getResourceId();
        ExternalizedComputeCluster externalizedComputeCluster =
                externalizedComputeClusterService.getExternalizedComputeCluster(resourceId);
        return Polling.stopAfterDelay(timeLimit, TimeUnit.HOURS)
                .stopIfException(false)
                .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                .run(() -> {
                    DefaultApi defaultApi = liftieService.getDefaultApi();
                    CommonClusterView liftieClusterView = defaultApi.getClusterStatus(externalizedComputeCluster.getLiftieName());
                    GithubInfraClouderaComLiftieLiftiePkgCommonStatusMessage clusterStatus = liftieClusterView.getClusterStatus();
                    LOGGER.debug("Cluster status: {}", clusterStatus);
                    if (clusterStatus != null && CREATING_STATUS.equals(clusterStatus.getStatus())) {
                        LOGGER.debug("{} cluster is in creating status", liftieClusterView.getClusterId());
                        return AttemptResults.justContinue();
                    } else if (clusterStatus == null) {
                        LOGGER.error("Cluster status is null");
                        return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, event.getData().getUserId(),
                                new RuntimeException("Cluster status is null")));
                    } else {
                        if (StringUtils.containsIgnoreCase(clusterStatus.getStatus(), "failed")) {
                            LOGGER.error("Cluster status is a failed status: {}", clusterStatus.getStatus());
                            String errorMessage = clusterStatus.getMessage();
                            return AttemptResults.finishWith(new ExternalizedComputeClusterCreateFailedEvent(resourceId, event.getData().getUserId(),
                                    new RuntimeException("Cluster creation failed. Status: " + clusterStatus.getStatus() + ". Message: " + errorMessage)));
                        } else {
                            LOGGER.debug("Cluster created successfully");
                            return AttemptResults.finishWith(
                                    new ExternalizedComputeClusterCreateWaitSuccessResponse(resourceId, event.getData().getUserId()));
                        }
                    }
                });
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterCreateWaitRequest.class);
    }
}
