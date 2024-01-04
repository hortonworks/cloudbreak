package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;

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
        return getRoleCrn(UmsRole.DBUS_UPLOADER, accountId).toString();
    }

    public String getBuiltInComputeMetricsPublisherRoleCrn(String accountId) {
        return getRoleCrn(UmsRole.COMPUTE_METRICS_PUBLISHER, accountId).toString();
    }

    public String getBuiltInOwnerResourceRoleCrn(String accountId) {
        return getResourceRoleCrn(UmsResourceRole.OWNER, accountId).toString();
    }

    public String getBuiltInEnvironmentAdminResourceRoleCrn(String accountId) {
        return getResourceRoleCrn(UmsResourceRole.ENVIRONMENT_ADMIN, accountId).toString();
    }

    public String getBuiltInEnvironmentUserResourceRoleCrn(String accountId) {
        return getResourceRoleCrn(UmsResourceRole.ENVIRONMENT_USER, accountId).toString();
    }

    public String getBuiltInWXMClusterAdminResourceRoleCrn(String accountId) {
        return getResourceRoleCrn(UmsResourceRole.WXM_CLUSTER_ADMIN, accountId).toString();
    }

    public Crn getResourceRoleCrn(UmsResourceRole umsResourceRole, String accountId) {
        Set<String> resourceRoles = grpcUmsClient.getResourceRoles(accountId);
        LOGGER.info("Resource roles in account {} are {}", accountId, Joiner.on(",").join(resourceRoles));
        return getCrnFromResourceRoles(umsResourceRole, resourceRoles);
    }

    public static Crn getCrnFromResourceRoles(UmsResourceRole umsResourceRole, Set<String> resourceRoles) {
        return resourceRoles.stream()
                .map(Crn::safeFromString)
                .filter(crn -> StringUtils.equals(crn.getResource(), umsResourceRole.getResourceRoleName()))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(
                        String.format("There is no resource role in UMS with name %s", umsResourceRole.getResourceRoleName())));
    }

    public Crn getRoleCrn(UmsRole umsRole, String accountId) {
        Set<String> roles = grpcUmsClient.getRoles(accountId);
        LOGGER.info("Roles in account {} are {}", accountId, Joiner.on(",").join(roles));
        return getCrnFromRoles(umsRole, roles);
    }

    public static Crn getCrnFromRoles(UmsRole umsRole, Set<String> roles) {
        return roles.stream()
                .map(Crn::safeFromString)
                .filter(crn -> StringUtils.equals(crn.getResource(), umsRole.getRoleName()))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(String.format("There is no role in UMS with name %s", umsRole.getRoleName())));
    }
}
