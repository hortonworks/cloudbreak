package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.datalake.service.sdx.DatabaseService.DURATION_IN_MINUTES_FOR_DB_POLLING;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.RdsWaitRequest;
import com.sequenceiq.datalake.flow.create.event.RdsWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;

@Component
public class RdsWaitHandler extends ExceptionCatcherEventHandler<RdsWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsWaitHandler.class);

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxCreateFailedEvent(resourceId, null, null, e);
    }

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        RdsWaitRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        String requestId = rdsWaitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        DetailedEnvironmentResponse env = rdsWaitRequest.getDetailedEnvironmentResponse();
        try {
            sdxClusterRepository.findById(sdxId).ifPresentOrElse(sdxCluster -> {
                if (sdxCluster.isCreateDatabase()) {
                    validForDatabaseCreation(sdxId, env);
                    LOGGER.debug("start polling database for sdx: {}", sdxId);
                    DatabaseServerStatusV4Response db = databaseService.create(sdxCluster, env, requestId);
                    setRdsCreatedStatus(sdxCluster);
                    sendEvent(new RdsWaitSuccessEvent(sdxId, userId, requestId, env, db), event);
                } else {
                    LOGGER.debug("skipping creation of database for sdx: {}", sdxId);
                    sendEvent(new RdsWaitSuccessEvent(sdxId, userId, requestId, env, null), event);
                }
            }, () -> {
                throw notFound("SDX cluster", sdxId).get();
            });
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Database polling exited before timeout. Cause: ", userBreakException);
            sendEvent(new SdxCreateFailedEvent(sdxId, userId, requestId, userBreakException), event);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            sendEvent(new SdxCreateFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Database creation timed out after " + DURATION_IN_MINUTES_FOR_DB_POLLING + " minutes")), event);
        } catch (PollerException exception) {
            LOGGER.info("Database polling failed for sdx: {}", sdxId, exception);
            sendEvent(new SdxCreateFailedEvent(sdxId, userId, requestId, exception), event);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database creation wait phase", anotherException);
            sendEvent(new SdxCreateFailedEvent(sdxId, userId, requestId, anotherException), event);
        }
    }

    private void setRdsCreatedStatus(SdxCluster cluster) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATED,
                ResourceEvent.SDX_RDS_CREATION_FINISHED, "Sdx external database created", cluster);
    }

    private void validForDatabaseCreation(Long sdxId, DetailedEnvironmentResponse env) {
        String message;
        if (env.getNetwork().getSubnetMetas().size() < 2) {
            message = String.format("Cannot create external database for sdx: %s, not enough subnets in the vpc", sdxId);
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
        Map<String, Long> zones = env.getNetwork().getSubnetMetas().values().stream()
                .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
        if (zones.size() < 2) {
            message = String.format("Cannot create external database for sdx: %s, the subnets in the vpc should be at least in two different availabilityzones",
                    sdxId);
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
    }

}
