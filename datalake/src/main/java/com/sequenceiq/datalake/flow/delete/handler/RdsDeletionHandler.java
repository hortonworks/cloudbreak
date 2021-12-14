package com.sequenceiq.datalake.flow.delete.handler;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class RdsDeletionHandler extends ExceptionCatcherEventHandler<RdsDeletionWaitRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsDeletionHandler.class);

    @Value("${sdx.db.operation.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Override
    public String selector() {
        return "RdsDeletionWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RdsDeletionWaitRequest> event) {
        return new SdxDeletionFailedEvent(resourceId, null, e, event.getData().isForced());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RdsDeletionWaitRequest> event) {
        RdsDeletionWaitRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        Selectable response;
        try {
            sdxClusterRepository.findById(sdxId).ifPresent(sdxCluster -> {
                if (sdxCluster.hasExternalDatabase() && StringUtils.isNotEmpty(sdxCluster.getDatabaseCrn())) {
                    LOGGER.debug("start polling database termination for sdx: {}", sdxId);
                    databaseService.terminate(sdxCluster, rdsWaitRequest.isForced());
                } else {
                    LOGGER.debug("skipping deletion of database for sdx: {}", sdxId);
                }
                setDeletedStatus(sdxCluster);
            });
            response = new RdsDeletionSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Database polling exited before timeout. Cause: ", userBreakException);
            response = new SdxDeletionFailedEvent(sdxId, userId, userBreakException, rdsWaitRequest.isForced());
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxDeletionFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database deletion timed out after " + durationInMinutes + " minutes"),
                    rdsWaitRequest.isForced());
        } catch (PollerException exception) {
            LOGGER.error("Database polling failed for sdx: {}", sdxId, exception);
            response = new SdxDeletionFailedEvent(sdxId, userId, exception, rdsWaitRequest.isForced());
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database deletion wait phase", anotherException);
            response = new SdxDeletionFailedEvent(sdxId, userId, anotherException, rdsWaitRequest.isForced());
        }
        return response;
    }

    private void setDeletedStatus(SdxCluster cluster) {
        try {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETED, "Datalake External RDS deleted", cluster);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Can not set status to DELETED because data lake was not found");
        }
        ownerAssignmentService.notifyResourceDeleted(cluster.getCrn(), MDCUtils.getRequestId());
    }
}
