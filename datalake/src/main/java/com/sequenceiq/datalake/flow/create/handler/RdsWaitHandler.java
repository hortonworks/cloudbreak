package com.sequenceiq.datalake.flow.create.handler;

import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.RdsWaitRequest;
import com.sequenceiq.datalake.flow.create.event.RdsWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.DatabaseService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;

@Component
public class RdsWaitHandler extends ExceptionCatcherEventHandler<RdsWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsWaitHandler.class);

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxCreateFailedEvent(resourceId, null, e);
    }

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        RdsWaitRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(sdxId);
        String userId = rdsWaitRequest.getUserId();
        DetailedEnvironmentResponse env = rdsWaitRequest.getDetailedEnvironmentResponse();
        Selectable response;
        try {
            if (sdxCluster.map(SdxCluster::getCreateDatabase).orElse(Boolean.FALSE)) {
                validForDatabaseCreation(sdxId, env);
                LOGGER.debug("start polling database for sdx: {}", sdxId);
                DatabaseServerStatusV4Response db = databaseService.create(sdxId, sdxCluster, env);
                response = new RdsWaitSuccessEvent(sdxId, userId, env, db);
            } else {
                LOGGER.debug("skipping creation of database for sdx: {}", sdxId);
                response = new RdsWaitSuccessEvent(sdxId, userId, env, null);
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Database polling exited before timeout. Cause: ", userBreakException);
            response = new SdxCreateFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxCreateFailedEvent(sdxId, userId, pollerStoppedException);
        } catch (PollerException exception) {
            LOGGER.info("Database polling failed for sdx: {}", sdxId, exception);
            response = new SdxCreateFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database creation wait phase", anotherException);
            response = new SdxCreateFailedEvent(sdxId, userId, anotherException);
        }
        sendEvent(response, event);
    }

    private void validForDatabaseCreation(Long sdxId, DetailedEnvironmentResponse env) {
        String message = "";
        if (env.getNetwork().getAws() == null) {
            message = "Cannot create external database for sdx: {}, for now only AWS is supported";
            LOGGER.debug(message, sdxId);
            throw new BadRequestException(message);
        }
        if (env.getNetwork().getSubnetMetas().size() < 2) {
            message = "Cannot create external database for sdx: {}, not enough subnets in the vpc";
            LOGGER.debug(message, sdxId);
            throw new BadRequestException(message);
        }
        Map<String, Long> zones = env.getNetwork().getSubnetMetas().values().stream()
                .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
        if (zones.size() < 2) {
            message = "Cannot create external database for sdx: {}, the subnets in the vpc should be at least in two different availabilityzones";
            LOGGER.debug(message, sdxId);
            throw new BadRequestException(message);
        }
        if (!Optional.of(env.getSecurityAccess()).map(SecurityAccessResponse::getDefaultSecurityGroupId).isPresent()) {
            message = "Cannot create external database for sdx: {}, there's no default securitygroup in the environment";
            LOGGER.debug(message, sdxId);
            throw new BadRequestException(message);
        }
    }

}
