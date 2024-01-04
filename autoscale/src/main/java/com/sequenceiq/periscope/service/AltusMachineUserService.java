package com.sequenceiq.periscope.service;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnEncoder;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.handler.FreeIpaCommunicator;

@Service
public class AltusMachineUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusMachineUserService.class);

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    @Value("${cb.userSync.version.threshold}")
    private String cloudbreakVersionThreshold;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private FreeIpaCommunicator freeIpaCommunicator;

    @Inject
    private CloudbreakVersionService cloudbreakVersionService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RoleCrnGenerator roleCrnGenerator;

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public void initializeMachineUserForEnvironment(Cluster cluster) {
        String environmentCrn;
        MachineUser machineUser = null;

        try {
            environmentCrn = cluster.getEnvironmentCrn();
            if (environmentCrn != null) {
                String accountId = Crn.safeFromString(environmentCrn).getAccountId();
                machineUser = getOrCreateAutoscaleMachineUser(environmentCrn, cluster.getClusterPertain().getTenant());
                String machineUserCrn = machineUser.getCrn();
                if (userSyncNeeded(cluster.getStackCrn())) {
                    Multimap<String, String> assignedResourceRoles =
                            grpcUmsClient.listAssignedResourceRoles(machineUserCrn);
                    String envUserResourceRoleCrn = roleCrnGenerator.getBuiltInEnvironmentUserResourceRoleCrn(accountId);
                    if (!assignedResourceRoles.get(environmentCrn).contains(envUserResourceRoleCrn)) {
                        grpcUmsClient.assignResourceRole(machineUserCrn, environmentCrn, envUserResourceRoleCrn);
                        LOGGER.info("Assigned resourcerole '{}' for  machineUserCrn '{}' for environment '{}'",
                                envUserResourceRoleCrn, machineUserCrn, environmentCrn);
                    }
                    syncEnvironment(cluster.getClusterPertain().getTenant(), machineUserCrn, environmentCrn, Optional.empty());
                }
                clusterService.setMachineUserCrn(cluster.getId(), machineUserCrn);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error initializing machineUser: {} for cluster '{}' yarn polling", machineUser, cluster.getStackCrn(), ex);
        }

    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public void deleteMachineUserForEnvironment(String accountId, String machineUserCrn, String environmentCrn) {
        if (environmentCrn != null && machineUserCrn != null) {
            MachineUser machineUser = getOrCreateAutoscaleMachineUser(environmentCrn, accountId);
            grpcUmsClient.deleteMachineUser(machineUser.getCrn(), accountId);
            syncEnvironment(accountId, machineUserCrn, environmentCrn, Optional.of(machineUser.getWorkloadUsername()));
            LOGGER.info("Deleted MachineUser for machineUserCrn '{}', environment '{}'", machineUserCrn, environmentCrn);
        }
    }

    private MachineUser getOrCreateAutoscaleMachineUser(String environmentCrn, String accountId) {
        //Idempotent api retrieves machine user or creates if missing.
        String autoscaleMachineUserName = CrnEncoder.generateMd5EncodedAutoscaleMachineUser(environmentCrn, false);
        MachineUser machineUser = grpcUmsClient.getOrCreateMachineUserWithoutAccessKey(autoscaleMachineUserName, accountId);
        LOGGER.info("Retrieved machineUser '{}' for machineUserName '{}' ", machineUser, autoscaleMachineUserName);
        return machineUser;
    }

    private void syncEnvironment(String accountId, String machineUserCrn, String environmentCrn, Optional<String> deletedWorkloadUserName) {
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setAccountId(accountId);
        request.setEnvironments(Set.of(environmentCrn));
        request.setMachineUsers(Set.of(machineUserCrn));
        deletedWorkloadUserName.ifPresent(s -> request.setDeletedWorkloadUsers(Set.of(s)));
        SyncOperationStatus statusResponse = freeIpaCommunicator.synchronizeAllUsers(request);
        LOGGER.info("Finished invoking freeIpa user Sync with operation: {}, for environmentCrn: {} and machineUserCrn: {}", statusResponse.getOperationId(),
                environmentCrn, machineUserCrn);
    }

    private boolean userSyncNeeded(String stackCrn) {
        String cbVersion = cloudbreakVersionService.getCloudbreakSaltStateVersionByStackCrn(stackCrn);
        if (!Strings.isNullOrEmpty(cbVersion)) {
            String shortened = StringUtils.substringBefore(cbVersion, "-");
            return VERSION_COMPARATOR.compare(() -> shortened, () -> cloudbreakVersionThreshold) < 0;
        }
        return true;
    }
}
