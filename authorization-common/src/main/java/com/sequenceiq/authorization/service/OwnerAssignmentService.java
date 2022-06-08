package com.sequenceiq.authorization.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class OwnerAssignmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private RoleCrnGenerator roleCrnGenerator;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void assignResourceOwnerRoleIfEntitled(String userCrn, String resourceCrn, String accountId) {
        try {
            umsClient.assignResourceRole(userCrn, resourceCrn, roleCrnGenerator.getBuiltInOwnerResourceRoleCrn(accountId),
                    regionAwareInternalCrnGeneratorFactory);
            LOGGER.debug("Owner role of {} is successfully assigned to the {} user", resourceCrn, userCrn);
        } catch (StatusRuntimeException ex) {
            if (Status.Code.ALREADY_EXISTS.equals(ex.getStatus().getCode())) {
                LOGGER.debug("Owner role of {} is already assigned to the {} user", resourceCrn, userCrn);
            } else {
                throw ex;
            }
        }
    }

    public void notifyResourceDeleted(String resourceCrn) {
        umsClient.notifyResourceDeleted(resourceCrn, regionAwareInternalCrnGeneratorFactory);
    }
}
