package com.sequenceiq.authorization.service;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class OwnerAssignmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private RoleCrnGenerator roleCrnGenerator;

    public void assignResourceOwnerRoleIfEntitled(String userCrn, String resourceCrn, String accountId) {
        try {
            if (entitlementService.isAuthorizationEntitlementRegistered(accountId)) {
                umsClient.assignResourceRole(userCrn, resourceCrn, roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(), MDCUtils.getRequestId());
                LOGGER.debug("Owner role of {} is successfully assigned to the {} user", resourceCrn, userCrn);
            }
        } catch (StatusRuntimeException ex) {
            if (Status.Code.ALREADY_EXISTS.equals(ex.getStatus().getCode())) {
                LOGGER.debug("Owner role of {} is already assigned to the {} user", resourceCrn, userCrn);
            } else {
                throw ex;
            }
        }
    }

    public void notifyResourceDeleted(String resourceCrn, Optional<String> requestId) {
        umsClient.notifyResourceDeleted(resourceCrn, requestId);
    }
}
