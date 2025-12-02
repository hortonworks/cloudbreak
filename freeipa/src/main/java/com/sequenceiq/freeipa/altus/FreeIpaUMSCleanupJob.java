package com.sequenceiq.freeipa.altus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.quartz.cleanup.job.UMSCleanupJob;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaUMSCleanupJob extends UMSCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUMSCleanupJob.class);

    @Inject
    private UMSCleanupConfig umsCleanupConfig;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private StackService stackService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        LOGGER.debug("Cleaning up unused UMS resources (machine users) has started.");
        Map<String, Set<String>> expectedMachineUsers = stackService
                .findAllRunning()
                .stream()
                .collect(Collectors.groupingBy(Stack::getAccountId,
                        Collectors.flatMapping(stack -> Stream.of(altusMachineUserService.getFluentMachineUser(stack),
                                altusMachineUserService.getMonitoringMachineUser(stack)), Collectors.toSet())));
        for (Map.Entry<String, Set<String>> machineUsersPerAccount : expectedMachineUsers.entrySet()) {
            String accountId = machineUsersPerAccount.getKey();
            List<UserManagementProto.MachineUser> machineUsers =
                    altusMachineUserService.getAllInternalMachineUsers(accountId);
            Set<String> machineUserValues = machineUsersPerAccount.getValue();
            for (UserManagementProto.MachineUser machineUser : machineUsers) {
                String name = machineUser.getMachineUserName();
                if ((name.startsWith("freeipa-fluent") || name.startsWith("freeipa-monitoring")) && !machineUserValues.contains(name)) {
                    clearMachineUser(accountId, machineUser, name);
                }
            }
        }
    }

    private void clearMachineUser(String accountId, UserManagementProto.MachineUser machineUser, String name) {
        LOGGER.debug("Cannot found stack for machine user {} (account: {}). Checks that if it can be deleted.",
                name, accountId);
        Instant beforeTime = Instant.now().minus(umsCleanupConfig.getMaxAgeDays(), ChronoUnit.DAYS);
        Instant creationTime = new Date(machineUser.getCreationDateMs()).toInstant();
        if (creationTime.isBefore(beforeTime)) {
            altusMachineUserService.cleanupMachineUser(name, accountId);
        } else {
            LOGGER.debug("Machine user with name {} is not old enough yet, skipping cleanup. (account id: {})",
                    name, accountId);
        }
    }
}
