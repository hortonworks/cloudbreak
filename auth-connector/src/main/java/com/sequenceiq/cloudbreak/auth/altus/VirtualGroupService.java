package com.sequenceiq.cloudbreak.auth.altus;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@Component
public class VirtualGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualGroupService.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private EntitlementService entitlementService;

    public Map<UmsVirtualGroupRight, String> createVirtualGroups(String accountId, String environmentCrn) {
        Map<UmsVirtualGroupRight, String> virtualGroups = new HashMap<>();
        for (UmsVirtualGroupRight right : UmsVirtualGroupRight.values()) {
            if (entitlementService.isEntitledForVirtualGroupRight(accountId, right)) {
                virtualGroups.put(right, createOrGetVirtualGroup(accountId, environmentCrn, right));
            }
        }
        return virtualGroups;
    }

    public String createOrGetVirtualGroup(VirtualGroupRequest virtualGroupRequest, UmsVirtualGroupRight right) {
        String virtualGroup;
        String adminGroup = virtualGroupRequest.getAdminGroup();
        if (StringUtils.isEmpty(adminGroup)) {
            if (entitlementService.isEntitledForVirtualGroupRight(virtualGroupRequest.getAccountId(), right)) {
                virtualGroup = createOrGetVirtualGroup(virtualGroupRequest.getAccountId(), virtualGroupRequest.getEnvironmentCrn(), right);
            } else {
                LOGGER.info("User is not entitled to create virtual group for right {} on environment {}", right, virtualGroupRequest.getEnvironmentCrn());
                virtualGroup = "";
            }
        } else {
            virtualGroup = adminGroup;
            LOGGER.info("Admingroup [{}] given by the user is used for {} right on {} environment", adminGroup, right, virtualGroupRequest.getEnvironmentCrn());
        }
        return virtualGroup;
    }

    public String getVirtualGroup(VirtualGroupRequest virtualGroupRequest, UmsVirtualGroupRight right) {
        if (entitlementService.isEntitledForVirtualGroupRight(virtualGroupRequest.getAccountId(), right)) {
            return getVirtualGroupFromUms(virtualGroupRequest.getAccountId(), virtualGroupRequest.getEnvironmentCrn(), right);
        }
        return virtualGroupRequest.getAdminGroup();
    }

    public void cleanupVirtualGroups(String accountId, String environmentCrn) {
        for (UmsVirtualGroupRight right : UmsVirtualGroupRight.values()) {
            try {
                LOGGER.debug("Start deleting virtual groups from UMS for environment '{}'", environmentCrn);
                grpcUmsClient.deleteWorkloadAdministrationGroupName(accountId, right, environmentCrn,
                        regionAwareInternalCrnGeneratorFactory);
                LOGGER.debug("Virtual groups deletion from UMS has been finished successfully for environment '{}'", environmentCrn);
            } catch (RuntimeException ex) {
                LOGGER.warn("UMS virtualgroup delete failed (this is not critical)", ex);
            }
        }
    }

    private String createOrGetVirtualGroup(String accountId, String environmentCrn, UmsVirtualGroupRight right) {
        String virtualGroup = getVirtualGroupFromUms(accountId, environmentCrn, right);
        if (StringUtils.isEmpty(virtualGroup)) {
            virtualGroup = grpcUmsClient.setWorkloadAdministrationGroupName(accountId, right, environmentCrn,
                    regionAwareInternalCrnGeneratorFactory);
            LOGGER.info("{} workloadAdministrationGroup is created for {} right on {} environment", virtualGroup, right, environmentCrn);
        } else {
            LOGGER.info("{} workloadAdministrationGroup is used for {} right on {} environment", virtualGroup, right, environmentCrn);
        }
        return virtualGroup;
    }

    private String getVirtualGroupFromUms(String accountId, String environmentCrn, UmsVirtualGroupRight right) {
        String virtualGroup = "";
        try {
            virtualGroup = grpcUmsClient.getWorkloadAdministrationGroupName(accountId, right, environmentCrn,
                    regionAwareInternalCrnGeneratorFactory);
        } catch (StatusRuntimeException ex) {
            if (Code.NOT_FOUND != ex.getStatus().getCode()) {
                throw ex;
            }
        }
        return virtualGroup;
    }
}