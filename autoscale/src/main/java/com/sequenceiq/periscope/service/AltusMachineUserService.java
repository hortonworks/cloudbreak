package com.sequenceiq.periscope.service;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.periscope.domain.Cluster;

@Service
public class AltusMachineUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final String AUTOSCALE_MACHINE_USER_NAME_PATTERN = "datahub-autoscale-metrics-%s";

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UserV1Endpoint userV1Endpoint;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RoleCrnGenerator roleCrnGenerator;

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public void initializeMachineUserForEnvironment(Cluster cluster) {
        try {
            String environmentCrn = cluster.getEnvironmentCrn();
            if (environmentCrn != null) {
                String machineUserCrn = getOrCreateAutoscaleMachineUser(environmentCrn, cluster.getClusterPertain().getTenant()).getCrn();
                Multimap<String, String> assignedResourceRoles =
                        grpcUmsClient.listAssignedResourceRoles(machineUserCrn, MDCUtils.getRequestId());
                if (!assignedResourceRoles.get(environmentCrn).contains(roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn())) {
                    grpcUmsClient.assignResourceRole(machineUserCrn, environmentCrn,
                            roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(), MDCUtils.getRequestId());
                    LOGGER.info("Assigned resourcerole '{}' for  machineUserCrn '{}' for environment '{}'",
                            roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(), machineUserCrn, environmentCrn);
                }
                syncEnvironment(cluster.getClusterPertain().getTenant(), machineUserCrn, environmentCrn, Optional.empty());
                clusterService.setMachineUserCrn(cluster.getId(), machineUserCrn);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error initializing machineUserCrn for cluster '{}' yarn polling", cluster.getStackCrn(), ex);
        }
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public void deleteMachineUserForEnvironment(String accountId, String machineUserCrn, String environmentCrn) {
        if (environmentCrn != null && machineUserCrn != null) {
            MachineUser machineUser = getOrCreateAutoscaleMachineUser(environmentCrn, accountId);
            grpcUmsClient.deleteMachineUser(machineUser.getCrn(), ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN, accountId, MDCUtils.getRequestId());
            syncEnvironment(accountId, machineUserCrn, environmentCrn, Optional.of(machineUser.getWorkloadUsername()));
            LOGGER.info("Deleted MachineUser for machineUserCrn '{}', environment '{}'", machineUserCrn, environmentCrn);
        }
    }

    private MachineUser getOrCreateAutoscaleMachineUser(String environmentCrn, String accountId) {
        //Idempotent api retrieves machine user or creates if missing.
        String autoscaleMachineUserName = String.format(AUTOSCALE_MACHINE_USER_NAME_PATTERN, Crn.fromString(environmentCrn).getResource());
        MachineUser machineUser = grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, accountId, MDCUtils.getRequestId());
        LOGGER.info("Retrieved machineUser '{}' for machineUserName '{}' ", machineUser, autoscaleMachineUserName);
        return machineUser;
    }

    private void syncEnvironment(String accountId, String machineUserCrn, String environmentCrn, Optional<String> deletedWorkloadUserName) {
        ThreadBasedUserCrnProvider.doAsInternalActor(() -> {
            SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
            request.setAccountId(accountId);
            request.setEnvironments(Set.of(environmentCrn));
            request.setMachineUsers(Set.of(machineUserCrn));
            if (deletedWorkloadUserName.isPresent()) {
                request.setDeletedWorkloadUsers(Set.of(deletedWorkloadUserName.get()));
            }
            userV1Endpoint.synchronizeAllUsers(request);
            LOGGER.info("Triggered freeipa sync for resourcerole '{}', machineUserCrn '{}', for environment '{}'",
                    roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(), machineUserCrn, environmentCrn);
        });
    }
}
