package com.sequenceiq.cloudbreak.auth.altus;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@Component
public class VirtualGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualGroupService.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Map<UmsRight, String> createVirtualGroups(String accountId, String environmentCrn) {
        Map<UmsRight, String> virtualGroups = new HashMap<>();
        for (UmsRight right : UmsRight.values()) {
            virtualGroups.put(right, createOrGetVirtualGroup(accountId, environmentCrn, right.getRight()));
        }
        return virtualGroups;
    }

    public String getVirtualGroup(VirtualGroupRequest virtualGroupRequest, String right) {
        String virtualGroup;
        String adminGroup = virtualGroupRequest.getAdminGroup();
        if (StringUtils.isEmpty(adminGroup)) {
            virtualGroup = createOrGetVirtualGroup(virtualGroupRequest.getAccountId(), virtualGroupRequest.getEnvironmentCrn(), right);
        } else {
            virtualGroup = adminGroup;
            LOGGER.info("Admingroup [{}] given by the user is used for {} right on {} environment", adminGroup, right, virtualGroupRequest.getEnvironmentCrn());
        }
        return virtualGroup;
    }

    public void cleanupVirtualGroups(String accountId, String environmentCrn) {
        for (UmsRight right : UmsRight.values()) {
            try {
                LOGGER.debug("Start deleting virtual groups from UMS for environment '{}'", environmentCrn);
                grpcUmsClient.deleteWorkloadAdministrationGroupName(accountId,
                        MDCUtils.getRequestId(), right.getRight(), environmentCrn,
                        regionAwareInternalCrnGeneratorFactory);
                LOGGER.debug("Virtual groups deletion from UMS has been finished successfully for environment '{}'", environmentCrn);
            } catch (RuntimeException ex) {
                LOGGER.warn("UMS virtualgroup delete failed (this is not critical)", ex);
            }
        }
    }

    private String createOrGetVirtualGroup(String accountId, String environmentCrn, String right) {
        String virtualGroup = "";
        try {
            virtualGroup = grpcUmsClient.getWorkloadAdministrationGroupName(accountId, MDCUtils.getRequestId(), right, environmentCrn,
                    regionAwareInternalCrnGeneratorFactory);
        } catch (StatusRuntimeException ex) {
            if (Code.NOT_FOUND != ex.getStatus().getCode()) {
                throw ex;
            }
        }
        if (StringUtils.isEmpty(virtualGroup)) {
            virtualGroup = grpcUmsClient.setWorkloadAdministrationGroupName(accountId,
                    MDCUtils.getRequestId(), right, environmentCrn, regionAwareInternalCrnGeneratorFactory);
            LOGGER.info("{} workloadAdministrationGroup is created for {} right on {} environment", virtualGroup, right, environmentCrn);
        } else {
            LOGGER.info("{} workloadAdministrationGroup is used for {} right on {} environment", virtualGroup, right, environmentCrn);
        }
        return virtualGroup;
    }
}
