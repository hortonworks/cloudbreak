package com.sequenceiq.cloudbreak.service.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ScheduledAccountPreferencesValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledAccountPreferencesValidator.class);

    private static final String EVERY_HOUR_0MIN_0SEC = "0 0 * * * *";

    @Inject
    private AccountPreferencesService accountPreferencesService;

    @Inject
    private StackService stackService;

    @Inject
    private AccountPreferencesValidator preferencesValidator;

    @Inject
    private ReactorFlowManager flowManager;

    @Scheduled(cron = EVERY_HOUR_0MIN_0SEC)
    public void validate() {
        LOGGER.info("Validate account preferences for all 'running' stack.");
        Map<String, AccountPreferences> accountPreferences = new HashMap<>();
        List<Stack> allAlive = stackService.getAllAlive();

        for (Stack stack : allAlive) {
            AccountPreferences preferences = getAccountPreferences(stack.getAccount(), accountPreferences);
            try {
                preferencesValidator.validateClusterTimeToLive(stack.getCreated(), preferences);
                preferencesValidator.validateUserTimeToLive(stack.getOwner(), preferences);
            } catch (AccountPreferencesValidationFailed e) {
                terminateStack(stack);
            }
        }
    }

    private AccountPreferences getAccountPreferences(String account, Map<String, AccountPreferences> accountPreferences) {
        if (accountPreferences.containsKey(account)) {
            return accountPreferences.get(account);
        } else {
            AccountPreferences preferences = accountPreferencesService.getByAccount(account);
            accountPreferences.put(account, preferences);
            return preferences;
        }
    }

    private void terminateStack(Stack stack) {
        if (!stack.isDeleteCompleted()) {
            LOGGER.info("Trigger termination of stack: '{}', owner: '{}', account: '{}'.", stack.getName(), stack.getOwner(), stack.getAccount());
            flowManager.triggerTermination(stack.getId());
        }
    }
}
