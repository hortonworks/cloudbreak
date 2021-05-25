package com.sequenceiq.cdp.databus.quartz;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cdp.databus.altus.DatabusMachineUserProvider;
import com.sequenceiq.cdp.databus.entity.AccountDatabusConfig;
import com.sequenceiq.cdp.databus.processor.AbstractDatabusRecordProcessor;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.TracedQuartzJob;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

import io.opentracing.Tracer;

@Component
public class DatabusCredentialCleanupJob extends TracedQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusCredentialCleanupJob.class);

    private final DatabusCredentialCleanuplJobConfig databusCredentialCleanuplJobConfig;

    private final AccountDatabusConfigService accountDatabusConfigService;

    private final DatabusMachineUserProvider databusMachineUserProvider;

    private final List<? extends AbstractDatabusRecordProcessor> databusRecordProcessors;

    public DatabusCredentialCleanupJob(Tracer tracer, DatabusCredentialCleanuplJobConfig databusCredentialCleanuplJobConfig,
            AccountDatabusConfigService accountDatabusConfigService, DatabusMachineUserProvider databusMachineUserProvider,
            List<? extends AbstractDatabusRecordProcessor> databusRecordProcessors) {
        super(tracer, "Account DataBus credential cleanup");
        this.databusCredentialCleanuplJobConfig = databusCredentialCleanuplJobConfig;
        this.accountDatabusConfigService = accountDatabusConfigService;
        this.databusMachineUserProvider = databusMachineUserProvider;
        this.databusRecordProcessors = databusRecordProcessors;
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            LOGGER.debug("Gather all account databus configs from db.");
            Iterable<AccountDatabusConfig> accountDatabusConfigs = accountDatabusConfigService.findAll();
            Set<String> accountsInUse = new HashSet<>();
            Map<String, String> nameAccessKeyPairsInDatabase = new HashMap<>();
            for (AccountDatabusConfig config: accountDatabusConfigs) {
                LOGGER.debug("Found machine user '{}' for account '{}' in db", config.getName(), config.getAccountId());
                accountsInUse.add(config.getAccountId());
                nameAccessKeyPairsInDatabase.put(config.getName(),
                        new Json(config.getDatabusCredential()).get(DataBusCredential.class).getAccessKey());
            }
            LOGGER.debug("Accounts that are in use by account databus credential table: {}", StringUtils.join(accountsInUse));
            for (String accountId : accountsInUse) {
                List<String> machineUserNamesForAccount = databusRecordProcessors.stream()
                        .map(p -> p.getMachineUserName(accountId)).collect(Collectors.toList());
                LOGGER.debug("Check the following machine user name access keys for account '{}' : {}",
                        accountId, StringUtils.join(machineUserNamesForAccount));
                checkMachineUsersForAccessKeyCleanup(nameAccessKeyPairsInDatabase, accountId, machineUserNamesForAccount);
            }
        } catch (IOException | TransactionService.TransactionExecutionException e) {
            LOGGER.error("Error during account databus credential cleanup.", e);
        }
    }

    private void checkMachineUsersForAccessKeyCleanup(Map<String, String> nameAccessKeyPairsInDatabase,
            String accountId, List<String> machineUserNamesForAccount) {
        for (String machineUserName : machineUserNamesForAccount) {
            cleanupUnusedAccessKeysForMachineUser(nameAccessKeyPairsInDatabase, accountId, machineUserName);
        }
    }

    private void cleanupUnusedAccessKeysForMachineUser(Map<String, String> nameAccessKeyPairsInDatabase, String accountId, String machineUserName) {
        Map<String, Long> accessKeyUsages = databusMachineUserProvider.getAccessKeyUsageMapForMachineUser(machineUserName, accountId);
        LOGGER.debug("Found {} access keys for {}", accessKeyUsages.size(), machineUserName);
        accessKeyUsages.forEach((accessKeyId, lastUsage) -> {
            cleanupOldAccessKey(nameAccessKeyPairsInDatabase, accountId, machineUserName, accessKeyId, lastUsage);
        });
    }

    private void cleanupOldAccessKey(Map<String, String> nameAccessKeyPairsInDatabase, String accountId,
            String machineUserName, String accessKeyId, Long lastUsage) {
        Instant beforeTime = Instant.now().minus(databusCredentialCleanuplJobConfig.getMaxAgeDays(), ChronoUnit.DAYS);
        Instant lastUpdateTime = new Date(lastUsage).toInstant();
        if (lastUpdateTime.isBefore(beforeTime)) {
            LOGGER.debug("Deleting access key with id {} from account {} (machine user: {})", accessKeyId, accountId, machineUserName);
            databusMachineUserProvider.deleteAccessKeyForMachineUser(accessKeyId, accountId);
            cleanupAccountDatabusConfigFromDb(nameAccessKeyPairsInDatabase, machineUserName, accessKeyId);
        }
    }

    private void cleanupAccountDatabusConfigFromDb(Map<String, String> nameAccessKeyPairsInDatabase, String machineUserName, String accessKeyId) {
        try {
            if (nameAccessKeyPairsInDatabase.containsKey(machineUserName)) {
                String accessKeyInDb = nameAccessKeyPairsInDatabase.get(machineUserName);
                if (StringUtils.isNotBlank(accessKeyInDb) && accessKeyInDb.equals(accessKeyId)) {
                    accountDatabusConfigService.deleteByName(machineUserName);
                }
            }
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.debug("Error during deleting account databus credential deletion", e);
        }
    }

    @Override
    protected Object getMdcContextObject() {
        return null;
    }
}
