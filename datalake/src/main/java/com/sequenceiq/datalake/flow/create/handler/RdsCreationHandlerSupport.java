package com.sequenceiq.datalake.flow.create.handler;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class RdsCreationHandlerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsCreationHandlerSupport.class);

    @Value("${sdx.db.operation.duration_min:80}")
    private int durationInMinutes;

    @Inject
    private SdxStatusService sdxStatusService;

    public void validForDatabaseCreation(Long sdxId, DetailedEnvironmentResponse env) {
        String message;
        if (CloudPlatform.AWS.name().equalsIgnoreCase(env.getCloudPlatform())) {
            if (env.getNetwork().getSubnetMetas().size() < 2) {
                message = String.format("Cannot create external database for Data Lake: %s, not enough subnets in the vpc", sdxId);
                LOGGER.error(message);
                throw new BadRequestException(message);
            }
            Map<String, Long> zones = env.getNetwork().getSubnetMetas().values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = String.format("Cannot create external database for Data Lake: %s, vpc subnets must cover at least two different availability zones",
                        sdxId);
                LOGGER.error(message);
                throw new BadRequestException(message);
            }
        }
    }

    public void setRdsCreatedStatus(SdxCluster cluster) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATED, "Data Lake external database created", cluster);
    }

    public Selectable toFailedEvent(Long sdxId, String userId, Exception exception) {
        if (exception instanceof UserBreakException) {
            LOGGER.error("Database polling exited before timeout. Cause: ", exception);
            return new SdxCreateFailedEvent(sdxId, userId, exception);
        } else if (exception instanceof PollerStoppedException) {
            LOGGER.error("Database poller stopped for Data Lake: {}", sdxId, exception);
            return new SdxCreateFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database creation timed out after " + durationInMinutes + " minutes"));
        } else if (exception instanceof PollerException) {
            LOGGER.error("Database polling failed for Data Lake: {}", sdxId, exception);
            return new SdxCreateFailedEvent(sdxId, userId, exception);
        } else {
            LOGGER.error("Something wrong happened in Data Lake database creation wait phase", exception);
            return new SdxCreateFailedEvent(sdxId, userId, exception);
        }
    }
}
