package com.sequenceiq.cdp.databus.service;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cdp.databus.altus.DatabusMachineUserProvider;
import com.sequenceiq.cdp.databus.cache.AccountDatabusConfigCache;
import com.sequenceiq.cdp.databus.entity.AccountDatabusConfig;
import com.sequenceiq.cdp.databus.model.exception.DatabusRecordProcessingException;
import com.sequenceiq.cdp.databus.repository.AccountDatabusConfigRepository;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@Service
public class AccountDatabusConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDatabusConfigService.class);

    private final AccountDatabusConfigRepository accountDatabusConfigRepository;

    private final TransactionService transactionService;

    private final DatabusMachineUserProvider databusMachineUserProvider;

    public AccountDatabusConfigService(AccountDatabusConfigRepository accountDatabusConfigRepository, TransactionService transactionService,
            DatabusMachineUserProvider databusMachineUserProvider) {
        this.accountDatabusConfigRepository = accountDatabusConfigRepository;
        this.transactionService = transactionService;
        this.databusMachineUserProvider = databusMachineUserProvider;
    }

    public Optional<AccountDatabusConfig> findByNameAndAccountId(final String name, final String accountId)
            throws TransactionService.TransactionExecutionException {
        return transactionService.required(() -> accountDatabusConfigRepository.findOneByNameAndAccountId(name, accountId));
    }

    public void save(final AccountDatabusConfig obj) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> accountDatabusConfigRepository.save(obj));
    }

    public void deleteByName(final String name) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> accountDatabusConfigRepository.deleteByName(name));
    }

    public Iterable<AccountDatabusConfig> findAll() throws TransactionService.TransactionExecutionException {
        return transactionService.required(accountDatabusConfigRepository::findAll);
    }

    /**
     * Obtain or create databus credential for specific account (by machine user name).
     * First, it will try to gather the credential from in-memory cache, if it cannot be found
     * in memory, then it will check the database. If it does not exist in the database either,
     * a new databus credential (machine user + access key) will be created, then stored in the
     * database and in-memory cache.
     * @param machineUserName machine user name for the databus credential.
     * @param accountId id of an account.
     * @param accountDatabusConfigCache cache that stores account level databus configs in memory.
     * @return created or gathered databus credential.
     * @throws DatabusRecordProcessingException error during obtaining databus credential.
     */
    public synchronized DataBusCredential getOrCreateDataBusCredentials(String machineUserName,
            String accountId, AccountDatabusConfigCache accountDatabusConfigCache) throws DatabusRecordProcessingException {
        DataBusCredential databusCredential;
        if (accountDatabusConfigCache.containsCredentialKey(accountId)) {
            LOGGER.debug("Found account databus credential in cache [accountId: {}, machineUser: {}]", accountId, machineUserName);
            databusCredential = accountDatabusConfigCache.getCredential(accountId);
        } else {
            LOGGER.debug("Not found account databus credential in cache [accountId: {}, machineUser: {}], check database for it...",
                    accountId, machineUserName);
            Optional<AccountDatabusConfig> accountDbusConfig;
            try {
                accountDbusConfig = findByNameAndAccountId(machineUserName, accountId);
            } catch (TransactionService.TransactionExecutionException e) {
                throw new DatabusRecordProcessingException(e);
            }
            if (accountDbusConfig.isPresent()) {
                LOGGER.debug("Found account databus config in db. [accountId: {}, machineUser: {}]", accountId, machineUserName);
                String dataBusCredentialRaw = accountDbusConfig.get().getDatabusCredential();
                try {
                    databusCredential = new Json(dataBusCredentialRaw).get(DataBusCredential.class);
                } catch (IOException e) {
                    throw new DatabusRecordProcessingException("Error during reading databus credential string from db.", e);
                }
            } else {
                LOGGER.debug("Not found account databus config in db. [accountId: {}, machineUser: {}]", accountId, machineUserName);
                Optional<AltusCredential> credentialOpt = databusMachineUserProvider.getOrCreateMachineUserAndAccessKey(machineUserName, accountId);
                if (credentialOpt.isPresent()) {
                    AltusCredential credential = credentialOpt.get();
                    LOGGER.info("Databus credential successfully created. [accountId: {}, machineUser: {}, accessKey: {}]",
                            accountId, machineUserName, credential.getAccessKey());
                    databusCredential = new DataBusCredential();
                    databusCredential.setMachineUserName(machineUserName);
                    databusCredential.setAccessKey(credential.getAccessKey());
                    databusCredential.setPrivateKey(new String(credential.getPrivateKey()));
                    AccountDatabusConfig newAccountDatabusConfig = new AccountDatabusConfig();
                    String dbusCredentialStr = new Json(databusCredential).getValue();
                    newAccountDatabusConfig.setDatabusCredential(dbusCredentialStr);
                    newAccountDatabusConfig.setName(machineUserName);
                    newAccountDatabusConfig.setAccountId(accountId);
                    try {
                        save(newAccountDatabusConfig);
                    } catch (TransactionService.TransactionExecutionException e) {
                        throw new DatabusRecordProcessingException(
                                String.format("Error during persisting account databus config in db. [accountId: %s, machineUser: %s]",
                                        accountId, machineUserName), e);
                    }
                } else {
                    throw new DatabusRecordProcessingException(
                            String.format("Databus credential cannot be created in UMS [accountId: %s, machineUser: %s]", accountId, machineUserName));
                }
            }
            LOGGER.debug("Putting databus credential into cache. [accountId: {}, machineUser: {}]", accountId, machineUserName);
            accountDatabusConfigCache.putCredential(accountId, databusCredential);
        }
        return databusCredential;
    }

    public synchronized boolean checkMachineUserWithAccessKeyStillExists(String accountId, AccountDatabusConfigCache accountDatabusConfigCache) {
        boolean result = false;
        if (accountDatabusConfigCache.containsCredentialKey(accountId)) {
            DataBusCredential dataBusCredential = accountDatabusConfigCache.getCredential(accountId);
            result = databusMachineUserProvider.isDataBusCredentialStillExist(accountId, dataBusCredential);
        }
        return result;
    }

    public synchronized void cleanupCacheAndDbForAccountIdAndName(String machineUserName, String accountId,
            AccountDatabusConfigCache accountDatabusConfigCache) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            Optional<AccountDatabusConfig> dbusConfigOpt = accountDatabusConfigRepository.findOneByNameAndAccountId(machineUserName, accountId);
            dbusConfigOpt.ifPresent(accountDatabusConfigRepository::delete);
        });
        if (accountDatabusConfigCache.containsCredentialKey(accountId)) {
            accountDatabusConfigCache.removeCredential(accountId);
        }
    }
}
