package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Service
public class RoleCrnGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCrnGenerator.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public String getBuiltInDatabusRoleCrn(String accountId) {
        return getRoleCrn("DbusUploader", accountId).toString();
    }

    public String getBuiltInOwnerResourceRoleCrn(String accountId) {
        return getResourceRoleCrn("Owner", accountId).toString();
    }

    public String getBuiltInEnvironmentAdminResourceRoleCrn(String accountId) {
        return getResourceRoleCrn("EnvironmentAdmin", accountId).toString();
    }

    public String getBuiltInEnvironmentUserResourceRoleCrn(String accountId) {
        return getResourceRoleCrn("EnvironmentUser", accountId).toString();
    }

    public String getBuiltInWXMClusterAdminResourceRoleCrn(String accountId) {
        return getResourceRoleCrn("WXMClusterAdmin", accountId).toString();
    }

    public Crn getResourceRoleCrn(String resourceRoleName, String accountId) {
        Set<String> resourceRoles = grpcUmsClient.getResourceRoles(accountId);
        LOGGER.info("Resource roles in account {} are {}", accountId, Joiner.on(",").join(resourceRoles));
        return resourceRoles.stream()
                .map(Crn::safeFromString)
                .filter(crn -> StringUtils.equals(crn.getResource(), resourceRoleName))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(String.format("There is no resource role in UMS with name %s", resourceRoleName)));
    }

    public Crn getRoleCrn(String roleName, String accountId) {
        Set<String> roles = grpcUmsClient.getRoles(accountId);
        LOGGER.info("Roles in account {} are {}", accountId, Joiner.on(",").join(roles));
        return roles.stream()
                .map(Crn::safeFromString)
                .filter(crn -> StringUtils.equals(crn.getResource(), roleName))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(String.format("There is no role in UMS with name %s", roleName)));
    }
}
