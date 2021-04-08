package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.RdsWaitRequest;
import com.sequenceiq.datalake.flow.create.event.RdsWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class RdsWaitHandler extends ExceptionCatcherEventHandler<RdsWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsWaitHandler.class);

    @Value("${sdx.db.operation.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RdsWaitRequest> event) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        RdsWaitRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        try {
            DetailedEnvironmentResponse env = environmentService.waitNetworkAndGetEnvironment(sdxId);
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
            if (sdxCluster.isPresent()) {
                if (sdxCluster.get().hasExternalDatabase()) {
                    validForDatabaseCreation(sdxId, env);
                    LOGGER.debug("start polling database for sdx: {}", sdxId);
                    databaseService.create(sdxCluster.get(), env);
                    setRdsCreatedStatus(sdxCluster.get());
                    return new RdsWaitSuccessEvent(sdxId, userId);
                } else {
                    LOGGER.debug("skipping creation of database for sdx: {}", sdxId);
                    return new RdsWaitSuccessEvent(sdxId, userId);
                }
            } else {
                throw notFound("SDX cluster", sdxId).get();
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Database polling exited before timeout. Cause: ", userBreakException);
            return new SdxCreateFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            return new SdxCreateFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database creation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Database polling failed for sdx: {}", sdxId, exception);
            return new SdxCreateFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database creation wait phase", anotherException);
            return new SdxCreateFailedEvent(sdxId, userId, anotherException);
        }
    }

    private void setRdsCreatedStatus(SdxCluster cluster) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATED, "Sdx external database created", cluster);
    }

    private void validForDatabaseCreation(Long sdxId, DetailedEnvironmentResponse env) {
        String message;
        if (CloudPlatform.AWS.name().equalsIgnoreCase(env.getCloudPlatform())) {
            if (env.getNetwork().getSubnetMetas().size() < 2) {
                message = String.format("Cannot create external database for sdx: %s, not enough subnets in the vpc", sdxId);
                LOGGER.error(message);
                throw new BadRequestException(message);
            }
            Map<String, Long> zones = env.getNetwork().getSubnetMetas().values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = String.format("Cannot create external database for sdx: %s, vpc subnets must cover at least two different availability zones",
                        sdxId);
                LOGGER.error(message);
                throw new BadRequestException(message);
            }
        }
    }

}
