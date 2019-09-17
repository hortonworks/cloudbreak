package com.sequenceiq.datalake.flow.delete.handler;

import static com.sequenceiq.datalake.service.sdx.DatabaseService.DURATION_IN_MINUTES_FOR_DB_POLLING;

import javax.inject.Inject;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.create.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.DatabaseService;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component
public class RdsDeletionHandler extends ExceptionCatcherEventHandler<RdsDeletionWaitRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsDeletionHandler.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private Clock clock;

    @Inject
    private SdxNotificationService notificationService;

    @Override
    public String selector() {
        return "RdsDeletionWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxDeletionFailedEvent(resourceId, null, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        RdsDeletionWaitRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        String requestId = rdsWaitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        Selectable response;
        try {
            sdxClusterRepository.findById(sdxId).ifPresent(sdxCluster -> {
                if (sdxCluster.isCreateDatabase() && Strings.isNotEmpty(sdxCluster.getDatabaseCrn())) {
                    LOGGER.debug("start polling database termination for sdx: {}", sdxId);
                    databaseService.terminate(sdxCluster, requestId, rdsWaitRequest.isForced());
                } else {
                    LOGGER.debug("skipping deletion of database for sdx: {}", sdxId);
                }
                setDeletedStatus(sdxCluster);
            });
            response = new RdsDeletionSuccessEvent(sdxId, userId, requestId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Database polling exited before timeout. Cause: ", userBreakException);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Database deletion timed out after " + DURATION_IN_MINUTES_FOR_DB_POLLING + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Database polling failed for sdx: {}", sdxId, exception);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database deletion wait phase", anotherException);
            response = new SdxDeletionFailedEvent(sdxId, userId, requestId, anotherException);
        }
        sendEvent(response, event);
    }

    private void setDeletedStatus(SdxCluster cluster) {
        sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.DELETED, "Datalake deleted", cluster);
        cluster.setDeleted(clock.getCurrentTimeMillis());
        sdxClusterRepository.save(cluster);
        notificationService.send(ResourceEvent.SDX_CLUSTER_DELETED, cluster);
    }
}
