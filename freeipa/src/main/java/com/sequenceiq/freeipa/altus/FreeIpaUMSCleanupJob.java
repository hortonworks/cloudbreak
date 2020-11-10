package com.sequenceiq.freeipa.altus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.quartz.cleanup.job.UMSCleanupJob;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

import io.opentracing.Tracer;

@Component
public class FreeIpaUMSCleanupJob extends UMSCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUMSCleanupJob.class);

    private final UMSCleanupConfig umsCleanupConfig;

    private final AltusMachineUserService altusMachineUserService;

    private final StackService stackService;

    public FreeIpaUMSCleanupJob(UMSCleanupConfig umsCleanupConfig, AltusMachineUserService altusMachineUserService,
            StackService stackService, Tracer tracer) {
        super(tracer, "FreeIpa UMS Cleanup Job");
        this.umsCleanupConfig = umsCleanupConfig;
        this.altusMachineUserService = altusMachineUserService;
        this.stackService = stackService;
    }

    @Override
    protected Object getMdcContextObject() {
        return null;
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        LOGGER.debug("Cleaning up unused UMS resources (machine users) has started.");
        Map<String, Set<String>> expectedMachineUsers = stackService
                .findAllRunning()
                .stream()
                .collect(Collectors.groupingBy(Stack::getAccountId,
                        Collectors.mapping(
                                altusMachineUserService::getFluentMachineUser,
                                Collectors.toSet()
                        )));
        for (Map.Entry<String, Set<String>> machineUsersPerAccount : expectedMachineUsers.entrySet()) {
            String accountId = machineUsersPerAccount.getKey();
            List<UserManagementProto.MachineUser> machineUsers =
                    altusMachineUserService.getAllInternalMachineUsers(accountId);
            Set<String> machineUserValues = machineUsersPerAccount.getValue();
            for (UserManagementProto.MachineUser machineUser : machineUsers) {
                String name = machineUser.getMachineUserName();
                if (name.startsWith("freeipa-fluent") && !machineUserValues.contains(name)) {
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
        }
    }
}
