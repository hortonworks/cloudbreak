package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.SudoCommand;
import com.sequenceiq.freeipa.client.model.SudoRule;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

@Service
public class SudoRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleService.class);

    private static final String HOST_CATEGORY = "all";

    @Value("${freeipa.sudo.ruleName}")
    private String ruleName;

    @Value("${freeipa.sudo.allowCommands:}")
    private Set<String> allowCommands;

    @Value("${freeipa.sudo.denyCommands:}")
    private Set<String> denyCommands;

    private Set<String> allCommands;

    @Inject
    private VirtualGroupService virtualGroupService;

    @PostConstruct
    public void postConstruct() {
        allCommands = Stream.concat(allowCommands.stream(), denyCommands.stream()).collect(Collectors.toSet());
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void setupSudoRule(StackUserSyncView stack, FreeIpaClient freeIpaClient) throws Exception {
        if (stack.isAvailable()) {
            String group = getSudoGroupName(stack.environmentCrn());
            if (!Strings.isNullOrEmpty(group)) {
                validateGroupExistsOnFreeIpa(group, freeIpaClient);

                LOGGER.info("Setup sudo rule '{}' to allow '{}' but deny '{}' commands for user group '{}' on all hosts.",
                        ruleName, allowCommands, denyCommands, group);

                SudoRule sudoRule = getOrCreateSudoRule(freeIpaClient);
                if (HOST_CATEGORY.equals(sudoRule.getHostCategory())) {
                    createMissingSudoCommands(sudoRule, freeIpaClient);
                    addMissingSudoRuleAllowCommands(sudoRule, freeIpaClient);
                    addMissingSudoRuleDenyCommands(sudoRule, freeIpaClient);
                    addMissingSudoUserGroup(sudoRule, group, freeIpaClient);
                    LOGGER.info("Sudo rule '{}' setup successfully finished.", ruleName);
                } else {
                    throw new IllegalStateException("Failed to setup sudo rule as '" + ruleName + "' is already exists but the host category is not 'all'.");
                }
            } else {
                LOGGER.warn("Setup sudo rule can not be performed as virtual group for '{}' right is not available.",
                        ALLOW_PRIVILEGED_OS_OPERATIONS.getRight());
            }
        } else {
            LOGGER.warn("Setup sudo rule can not be performed as stack '{}' is not in available state.", stack.resourceCrn());
        }
    }

    private void validateGroupExistsOnFreeIpa(String group, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        freeIpaClient.groupShow(group);
    }

    private String getSudoGroupName(String environmentCrn) {
        VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(environmentCrn, null);
        return virtualGroupService.getVirtualGroup(virtualGroupRequest, ALLOW_PRIVILEGED_OS_OPERATIONS);
    }

    private void createMissingSudoCommands(SudoRule sudoRule, FreeIpaClient freeIpaClient) throws Exception {
        List<String> sudoCommands = freeIpaClient.sudoCommandFindAll().stream().map(SudoCommand::getSudocmd).collect(Collectors.toList());
        Set<String> missingSudoCommands = getMissingCommands(allCommands, sudoCommands);
        LOGGER.info("Create sudo command(s) '{}' to have '{}' command(s) available for sudo rule '{}'.", missingSudoCommands, allCommands, sudoRule);
        addSudoCommands(missingSudoCommands, freeIpaClient);
    }

    private void addMissingSudoRuleAllowCommands(SudoRule sudoRule, FreeIpaClient freeIpaClient) throws Exception {
        Set<String> missingSudoRuleAllowCommands = getMissingCommands(allowCommands, sudoRule.getAllowSudoCommands());
        LOGGER.info("Add missing allow command(s) '{}' to have '{}' command(s) allowed for sudo rule '{}'",
                missingSudoRuleAllowCommands, allowCommands, sudoRule);
        addSudoRuleAllowCommands(missingSudoRuleAllowCommands, freeIpaClient);
    }

    private void addMissingSudoRuleDenyCommands(SudoRule sudoRule, FreeIpaClient freeIpaClient) throws Exception {
        Set<String> missingSudoRuleDenyCommands = getMissingCommands(denyCommands, sudoRule.getDenySudoCommands());
        LOGGER.info("Add missing deny command(s) '{}' to have '{}' command(s) denied for sudo rule '{}'", missingSudoRuleDenyCommands, denyCommands, sudoRule);
        addSudoRuleDenyCommands(missingSudoRuleDenyCommands, freeIpaClient);
    }

    private void addMissingSudoUserGroup(SudoRule sudoRule, String userGroup, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        if (!sudoRule.getUserGroups().contains(userGroup)) {
            LOGGER.info("Add user group '{}' to sudo rule '{}'", userGroup, sudoRule);
            freeIpaClient.sudoRuleAddGroup(ruleName, userGroup);
        } else {
            LOGGER.info("User group '{}' already added to sudo rule '{}'", userGroup, sudoRule);
        }
    }

    private Set<String> getMissingCommands(Set<String> requiredCommands, List<String> existingCommands) {
        return requiredCommands
                .stream()
                .filter(requiredCommand -> existingCommands
                        .stream()
                        .noneMatch(existingCommand -> Objects.equals(existingCommand, requiredCommand)))
                .collect(Collectors.toSet());
    }

    private SudoRule getOrCreateSudoRule(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
            Optional<SudoRule> result = freeIpaClient.sudoRuleShow(ruleName);
            if (result.isEmpty()) {
                return freeIpaClient.sudoRuleAdd(ruleName, true);
            } else {
                return result.get();
            }
    }

    private void addSudoCommands(Set<String> sudoCommands, FreeIpaClient freeIpaClient) throws Exception {
        for (String sudoCommand : sudoCommands) {
            freeIpaClient.sudoCommandAdd(sudoCommand);
        }
    }

    private void addSudoRuleAllowCommands(Set<String> commands, FreeIpaClient freeIpaClient) throws Exception {
        for (String command : commands) {
            freeIpaClient.sudoRuleAddAllowCommand(ruleName, command);
        }
    }

    private void addSudoRuleDenyCommands(Set<String> commands, FreeIpaClient freeIpaClient) throws Exception {
        for (String command : commands) {
            freeIpaClient.sudoRuleAddDenyCommand(ruleName, command);
        }
    }
}
