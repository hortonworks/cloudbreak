package com.sequenceiq.cloudbreak.service.account;

import javax.inject.Inject;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;
import org.springframework.stereotype.Component;

@Component
public class AccountPreferencesValidator {
    private static final Long EXTREMAL_VALUE = 0L;

    @Inject
    private AccountPreferencesService accountPreferencesService;

    @Inject
    private StackService stackService;

    @Inject
    private UserDetailsService userDetailsService;

    public void validate(Stack stack, String account, String owner) throws AccountPreferencesValidationFailed {
        validate(stack.getInstanceGroups(), stack.getFullNodeCount(), account, owner);
    }

    public void validate(Long stackId, Integer scalingAdjustment, String account, String owner) throws AccountPreferencesValidationFailed {
        Stack stack = stackService.getById(stackId);
        Integer newNodeCount = stack.getFullNodeCount() + scalingAdjustment;
        validate(stack.getInstanceGroups(), newNodeCount, account, owner);
    }

    private void validate(Set<InstanceGroup> instanceGroups, Integer nodeCount, String account, String owner) throws AccountPreferencesValidationFailed {
        AccountPreferences preferences = accountPreferencesService.getByAccount(account);
        validateNumberOfNodesPerCluster(nodeCount, preferences);
        validateNumberOfClusters(account, preferences);
        validateAllowedInstanceTypes(instanceGroups, preferences);
        validateNumberOfClustersPerUser(owner, preferences);
        validateUserTimeToLive(owner, preferences);
    }

    private void validateNumberOfNodesPerCluster(Integer nodeCount, AccountPreferences preferences) throws AccountPreferencesValidationFailed {
        Long maxNodeNumberPerCluster = preferences.getMaxNumberOfNodesPerCluster();
        if (needToValidateField(maxNodeNumberPerCluster) && nodeCount > maxNodeNumberPerCluster) {
            throw new AccountPreferencesValidationFailed(String.format("Error: Cluster with maximum '%s' instances could be created within this account!",
                    maxNodeNumberPerCluster));
        }
    }

    private void validateNumberOfClusters(String account, AccountPreferences preferences) throws AccountPreferencesValidationFailed {
        Long maxNumberOfClusters = preferences.getMaxNumberOfClusters();
        if (needToValidateField(maxNumberOfClusters)) {
            Set<Stack> stacks = stackService.retrieveAccountStacks(account);
            if (stacks.size() >= maxNumberOfClusters) {
                throw new AccountPreferencesValidationFailed(
                        String.format("No more cluster could be created! The number of clusters exceeded the account's limit(%s)!", maxNumberOfClusters));
            }
        }
    }

    private void validateAllowedInstanceTypes(Set<InstanceGroup> instanceGroups, AccountPreferences preferences) throws AccountPreferencesValidationFailed {
        List<String> allowedInstanceTypes = preferences.getAllowedInstanceTypes();
        if (needToValidateField(allowedInstanceTypes)) {
            for (InstanceGroup ig : instanceGroups) {
                String instanceTypeName = ig.getTemplate().getInstanceTypeName();
                if (!allowedInstanceTypes.contains(instanceTypeName)) {
                    throw new AccountPreferencesValidationFailed(
                            String.format("Error: The '%s' instance type isn't allowed within the account!", instanceTypeName));
                }
            }
        }
    }

    private void validateNumberOfClustersPerUser(String owner, AccountPreferences preferences) throws AccountPreferencesValidationFailed {
        Long maxClustersPerUser = preferences.getMaxNumberOfClustersPerUser();
        if (needToValidateField(maxClustersPerUser)) {
            Set<Stack> stacks = stackService.retrieveOwnerStacks(owner);
            if (stacks.size() >= maxClustersPerUser) {
                throw new AccountPreferencesValidationFailed(
                        String.format("No more cluster could be created! The number of clusters exceeded the user's limit(%s)!", maxClustersPerUser));
            }
        }
    }

    private void validateUserTimeToLive(String owner, AccountPreferences preferences) throws AccountPreferencesValidationFailed {
        Long userTimeToLive = preferences.getUserTimeToLive();
        if (needToValidateField(userTimeToLive)) {
            CbUser cbUser = userDetailsService.getDetails(owner, UserFilterField.USERID);
            long now = Calendar.getInstance().getTimeInMillis();
            long userActiveTime = now - cbUser.getCreated().getTime();
            if (userActiveTime > userTimeToLive) {
                throw new AccountPreferencesValidationFailed("Cluster could no be created, the user demo time is expired!");
            }
        }
    }

    private boolean needToValidateField(long field) {
        return !EXTREMAL_VALUE.equals(field);
    }

    private boolean needToValidateField(List<String> field) {
        return !field.isEmpty();
    }
}