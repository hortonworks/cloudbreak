package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

@Component
public class MachineUserRetriever {
    @VisibleForTesting
    static final boolean DONT_INCLUDE_INTERNAL_MACHINE_USERS = false;

    @VisibleForTesting
    static final boolean INCLUDE_WORKLOAD_MACHINE_USERS = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineUserRetriever.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public List<UserManagementProto.MachineUser> getMachineUsers(
            String actorCrn, String accountId, Optional<String> requestIdOptional,
            boolean fullSync, Set<String> machineUserCrns,
            BiConsumer<String, String> warnings) {
        if (fullSync) {
            return grpcUmsClient.listAllMachineUsers(actorCrn, accountId,
                    DONT_INCLUDE_INTERNAL_MACHINE_USERS, INCLUDE_WORKLOAD_MACHINE_USERS,
                    requestIdOptional);
        } else if (!machineUserCrns.isEmpty()) {
            return getRequestedMachineUsers(actorCrn, accountId, machineUserCrns, requestIdOptional, warnings);
        } else {
            return List.of();
        }
    }

    private List<UserManagementProto.MachineUser> getRequestedMachineUsers(
            String actorCrn, String accountId,
            Set<String> machineUserCrns, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        try {
            return grpcUmsClient.listMachineUsers(actorCrn, accountId, List.copyOf(machineUserCrns),
                    DONT_INCLUDE_INTERNAL_MACHINE_USERS, INCLUDE_WORKLOAD_MACHINE_USERS,
                    requestIdOptional);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("Some requested machine user not found in UMS. " +
                                "Attempting to retrieve machine users individually.",
                        e.getLocalizedMessage());
                return getRequestedMachineUsersIndividually(
                        actorCrn, accountId, machineUserCrns, requestIdOptional, warnings);
            } else {
                throw e;
            }
        }
    }

    private List<UserManagementProto.MachineUser> getRequestedMachineUsersIndividually(
            String actorCrn, String accountId,
            Set<String> machineUserCrns, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        List<UserManagementProto.MachineUser> machineUsers = Lists.newArrayListWithCapacity(machineUserCrns.size());
        for (String machineUserCrn : machineUserCrns) {
            try {
                machineUsers.addAll(grpcUmsClient.listMachineUsers(actorCrn, accountId, List.of(machineUserCrn),
                        DONT_INCLUDE_INTERNAL_MACHINE_USERS, INCLUDE_WORKLOAD_MACHINE_USERS,
                        requestIdOptional));
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                    LOGGER.warn("Machine user CRN {} not found in UMS. Machine user will not be added to the UMS Users State. {}",
                            machineUserCrn, e.getLocalizedMessage());
                    warnings.accept(machineUserCrn, String.format("Machine User %s not found.", machineUserCrn));
                } else {
                    throw e;
                }
            }
        }
        return machineUsers;
    }
}
