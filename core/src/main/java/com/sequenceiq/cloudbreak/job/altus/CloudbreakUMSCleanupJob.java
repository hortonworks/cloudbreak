package com.sequenceiq.cloudbreak.job.altus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.quartz.cleanup.job.UMSCleanupJob;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class CloudbreakUMSCleanupJob extends UMSCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUMSCleanupJob.class);

    private static final String DATABUS_TP_MACHINUE_USER_PATTERN = "datahub-wa-publisher-%s";

    private final UMSCleanupConfig umsCleanupConfig;

    private final AltusMachineUserService altusMachineUserService;

    private final StackService stackService;

    public CloudbreakUMSCleanupJob(UMSCleanupConfig umsCleanupConfig, AltusMachineUserService altusMachineUserService,
        StackService stackService) {
        this.umsCleanupConfig = umsCleanupConfig;
        this.altusMachineUserService = altusMachineUserService;
        this.stackService = stackService;
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        LOGGER.debug("Cleaning up unused UMS resources (machine users) has started.");
        List<Crn> allStackCrns = getAllStackCrns();
        String datalakeName = Crn.Service.DATALAKE.getName();
        Map<String, Set<String>> datalakeMachineUsers = getPossibleDatalakeMachineUsers(allStackCrns, datalakeName);
        String datahubName = Crn.Service.DATAHUB.getName();
        List<DatahubMachineUsers> datahubMachineUsers = getPossibleDatahubMachineUsers(allStackCrns, datahubName);
        Map<String, Set<String>> datahubFluentUsers = getPossibleDatahubFluentUsers(datahubMachineUsers);
        Map<String, Set<String>> datahubTpUsers = getPossibleDatahubTpUsers(datahubMachineUsers);
        Map<String, Set<String>> datahubMonitoringUsers = getPossibleDatahubMonitoringUsers(datahubMachineUsers);
        for (Map.Entry<String, Set<String>> entry : datalakeMachineUsers.entrySet()) {
            String accountId = entry.getKey();
            List<UserManagementProto.MachineUser> machineUsers =
                    altusMachineUserService.getAllInternalMachineUsers(accountId);
            Set<String> datalakeMachineUsersForAccount = entry.getValue();
            for (UserManagementProto.MachineUser machineUser : machineUsers) {
                String name = machineUser.getMachineUserName();
                if (shouldDeleteMachineUser(name,
                        datalakeMachineUsersForAccount,
                        datahubFluentUsers.getOrDefault(accountId, new HashSet<>()),
                        datahubTpUsers.getOrDefault(accountId, new HashSet<>()),
                        datahubMonitoringUsers.getOrDefault(accountId, new HashSet<>()))) {
                    cleanupMachineUser(accountId, machineUser, name);
                }
            }
        }
        LOGGER.debug("Cleaning up unused UMS resources (machine users) has finished.");
    }

    private boolean shouldDeleteMachineUser(String name,
                                            Set<String> datalakeMachineUsers,
                                            Set<String> datahubFluentMachineUsers,
                                            Set<String> datahubTPMachineUsers,
                                            Set<String> datahubMonitoringMachineUsers) {
        if ((name.startsWith("datalake-fluent") || name.startsWith("datalake-monitoring")) && !datalakeMachineUsers.contains(name)) {
            return true;
        } else if (name.startsWith("datahub-fluent") && !datahubFluentMachineUsers.contains(name)) {
            return true;
        } else if (name.startsWith("datahub-wa-publisher") && !datahubTPMachineUsers.contains(name)) {
            return true;
        } else if (name.startsWith("datahub-monitoring") && !datahubMonitoringMachineUsers.contains(name)) {
            return true;
        }
        return false;
    }

    private Map<String, Set<String>> getPossibleDatahubTpUsers(List<DatahubMachineUsers> datahubMachineUsers) {
        return datahubMachineUsers
                .stream()
                .collect(Collectors.groupingBy(DatahubMachineUsers::getAccountId,
                        Collectors.mapping(
                                DatahubMachineUsers::getTelemetryPublisherUser,
                                Collectors.toSet()
                        )));
    }

    private Map<String, Set<String>> getPossibleDatahubFluentUsers(List<DatahubMachineUsers> datahubMachineUsers) {
        return datahubMachineUsers
                .stream()
                .collect(Collectors.groupingBy(DatahubMachineUsers::getAccountId,
                        Collectors.mapping(
                                DatahubMachineUsers::getFluentUser,
                                Collectors.toSet()
                        )));
    }

    private Map<String, Set<String>> getPossibleDatahubMonitoringUsers(List<DatahubMachineUsers> datahubMachineUsers) {
        return datahubMachineUsers
                .stream()
                .collect(Collectors.groupingBy(DatahubMachineUsers::getAccountId,
                        Collectors.mapping(
                                DatahubMachineUsers::getMonitoringUser,
                                Collectors.toSet()
                        )));
    }

    private List<DatahubMachineUsers> getPossibleDatahubMachineUsers(List<Crn> allStackCrns, String datahubName) {
        return allStackCrns
                .stream()
                .filter(crn -> datahubName.equals(crn.getService().getName()))
                .map(crn -> {
                    String fluentUserName = altusMachineUserService.getFluentDatabusMachineUserName(datahubName, crn.getResource());
                    String tpUserName = String.format(DATABUS_TP_MACHINUE_USER_PATTERN, crn.getResource());
                    String monitoringUser = altusMachineUserService.getMonitoringMachineUserName(datahubName, crn.getResource());
                    return new DatahubMachineUsers(crn.getAccountId(), fluentUserName, tpUserName, monitoringUser);
                })
                .collect(Collectors.toList());
    }

    private Map<String, Set<String>> getPossibleDatalakeMachineUsers(List<Crn> allStackCrns, String datalakeName) {
        return allStackCrns
                .stream()
                .filter(crn -> datalakeName.equals(crn.getService().getName()))
                .collect(Collectors.groupingBy(Crn::getAccountId, Collectors.flatMapping(
                        s -> Stream.of(
                                altusMachineUserService.getFluentDatabusMachineUserName(datalakeName, s.getResource()),
                                altusMachineUserService.getMonitoringMachineUserName(datalakeName, s.getResource())),
                        Collectors.toSet())));
    }

    private List<Crn> getAllStackCrns() {
        return stackService
                .getAllAlive()
                .stream()
                .map(s -> Crn.fromString(s.getCrn()))
                .collect(Collectors.toList());
    }

    private void cleanupMachineUser(String accountId, UserManagementProto.MachineUser machineUser, String name) {
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
