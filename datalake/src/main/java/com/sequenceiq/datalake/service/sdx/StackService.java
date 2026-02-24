package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public StackV4Response getDetail(String name, Set<String> entries, String accountId) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details by name {}", name);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.get(WORKSPACE_ID_DEFAULT, name, entries, accountId));
        } catch (jakarta.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }

    public StackV4Response getDetailWithResources(String name, Set<String> entries, String accountId) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details with resources by name {}", name);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.getWithResources(WORKSPACE_ID_DEFAULT, name, entries, accountId));
        } catch (jakarta.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }

    public RangerRazEnabledV4Response rangerRazEnabledInternal(String clusterCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.rangerRazEnabledInternal(WORKSPACE_ID_DEFAULT, clusterCrn, initiatorUserCrn));
    }

    public FlowIdentifier sync(String name, String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.sync(WORKSPACE_ID_DEFAULT, name, accountId));
    }

    public FlowIdentifier modifyProxyConfigInternal(String clusterCrn, String previousProxyConfigCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                initiatorUserCrn -> stackV4Endpoint.modifyProxyConfigInternal(WORKSPACE_ID_DEFAULT, clusterCrn,
                        previousProxyConfigCrn, initiatorUserCrn));
    }

    public void validateRdsSslCertRotation(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String operationNameToValidate = "RDS SSL certificate rotation";
        Runnable endpointCall = () -> stackV4Endpoint.validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, crn, userCrn);
        validateOnCoreInternalApiWithExceptionHandling(endpointCall, operationNameToValidate);
    }

    public void validateDefaultJavaVersionUpdate(String crn, SetDefaultJavaVersionRequest request) {
        String operationNameToValidate = "default Java version update";
        Runnable endpointCall = () -> stackV4Endpoint.validateDefaultJavaVersionUpdateByCrnInternal(WORKSPACE_ID_DEFAULT, crn, request);
        validateOnCoreInternalApiWithExceptionHandling(endpointCall, operationNameToValidate);
    }

    private void validateOnCoreInternalApiWithExceptionHandling(Runnable endpointCall, String operationNameToValidate) {
        LOGGER.info("Calling Cloudbreak to validate {} is triggerable", operationNameToValidate);
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(endpointCall);
        } catch (jakarta.ws.rs.BadRequestException e) {
            String msg = String.format("Validation failed %s is not triggerable", operationNameToValidate);
            LOGGER.info(msg, e);
            String message = Optional.ofNullable(e.getResponse())
                    .map(resp -> resp.hasEntity() ? resp.readEntity(ExceptionResponse.class) : new ExceptionResponse(msg))
                    .map(ExceptionResponse::getMessage)
                    .orElse(msg);
            throw new BadRequestException(message, e);
        } catch (jakarta.ws.rs.WebApplicationException wae) {
            String msg = String.format("Failed to validate %s for SDX", operationNameToValidate);
            LOGGER.warn(msg, wae);
            throw new IllegalStateException(msg, wae);
        }
    }

    public List<String> listAvailableJavaVersions(String crn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.listAvailableJavaVersionsByCrnInternal(WORKSPACE_ID_DEFAULT, crn));
    }

    public void modifyNotificationState(SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.modifyNotificationStateByCrn(
                        WORKSPACE_ID_DEFAULT,
                        sdxCluster.getCrn(),
                        initiatorUserCrn
                )
        );
    }
}