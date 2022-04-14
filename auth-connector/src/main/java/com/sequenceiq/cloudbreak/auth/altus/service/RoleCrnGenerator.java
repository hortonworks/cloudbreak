package com.sequenceiq.cloudbreak.auth.altus.service;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
public class RoleCrnGenerator {

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public String getBuiltInDatabusRoleCrn() {
        return getRoleCrn("DbusUploader").toString();
    }

    public String getBuiltInOwnerResourceRoleCrn() {
        return getResourceRoleCrn("Owner").toString();
    }

    public String getBuiltInEnvironmentAdminResourceRoleCrn() {
        return getResourceRoleCrn("EnvironmentAdmin").toString();
    }

    public Crn getResourceRoleCrn(String resourceRoleName) {
        return grpcUmsClient.getResourceRoles(ThreadBasedUserCrnProvider.getAccountId(), MDCUtils.getRequestId()).stream()
                .map(Crn::safeFromString)
                .filter(crn -> StringUtils.equals(crn.getResource(), resourceRoleName))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(String.format("There is no resource role in UMS with name %s", resourceRoleName)));
    }

    public Crn getRoleCrn(String roleName) {
        return grpcUmsClient.getRoles(ThreadBasedUserCrnProvider.getAccountId(), MDCUtils.getRequestId()).stream()
                .map(Crn::safeFromString)
                .filter(crn -> StringUtils.equals(crn.getResource(), roleName))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(String.format("There is no role in UMS with name %s", roleName)));
    }
}
