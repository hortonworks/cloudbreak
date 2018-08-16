package com.sequenceiq.cloudbreak.service.account;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@Component
public class AccountPreferencesValidator {
    private static final Long EXTREMAL_VALUE = 0L;

    @Inject
    private AccountPreferencesService accountPreferencesService;

    @Inject
    private StackService stackService;

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    public void validate(Stack stack, String account, String owner) throws AccountPreferencesValidationException {
        validate(stack.getInstanceGroups(), stack.getFullNodeCount(), account, owner);
    }

    public void validate(Long stackId, Integer scalingAdjustment) throws AccountPreferencesValidationException {
        Stack stack = stackService.getByIdWithListsWithoutAuthorization(stackId);
        Integer newNodeCount = stack.getFullNodeCount() + scalingAdjustment;
        validate(stack.getInstanceGroups(), newNodeCount, stack.getAccount(), stack.getOwner());
    }

    private void validate(Iterable<InstanceGroup> instanceGroups, Integer nodeCount, String account, String owner)
            throws AccountPreferencesValidationException {
        AccountPreferences preferences = accountPreferencesService.getByAccount(account);
        validateNumberOfNodesPerCluster(nodeCount, preferences);
//        validateNumberOfClusters(account, preferences);
        validateAllowedInstanceTypes(instanceGroups, preferences);
//        validateNumberOfClustersPerUser(owner, preferences);
        validateUserTimeToLive(owner, preferences);
    }

    private void validateNumberOfNodesPerCluster(Integer nodeCount, AccountPreferences preferences) throws AccountPreferencesValidationException {
        Long maxNodeNumberPerCluster = preferences.getMaxNumberOfNodesPerCluster();
        if (needToValidateField(maxNodeNumberPerCluster) && nodeCount > maxNodeNumberPerCluster) {
            throw new AccountPreferencesValidationException(String.format("Cluster with maximum '%s' instances could be created within this account!",
                    maxNodeNumberPerCluster));
        }
    }

//    private void validateNumberOfClusters(String account, AccountPreferences preferences) throws AccountPreferencesValidationException {
//        Long maxNumberOfClusters = preferences.getMaxNumberOfClusters();
//        if (needToValidateField(maxNumberOfClusters)) {
//            Set<Stack> stacks = stackService.retrieveAccountStacks(account);
//            if (stacks.size() >= maxNumberOfClusters) {
//                throw new AccountPreferencesValidationException(
//                        String.format("No more cluster could be created! The number of clusters exceeded the account's limit(%s)!", maxNumberOfClusters));
//            }
//        }
//    }

    private void validateAllowedInstanceTypes(Iterable<InstanceGroup> instanceGroups, AccountPreferences preferences)
            throws AccountPreferencesValidationException {
        List<String> allowedInstanceTypes = preferences.getAllowedInstanceTypes();
        if (needToValidateField(allowedInstanceTypes)) {
            for (InstanceGroup ig : instanceGroups) {
                String instanceTypeName = ig.getTemplate().getInstanceType();
                if (!allowedInstanceTypes.contains(instanceTypeName)) {
                    throw new AccountPreferencesValidationException(
                            String.format("The '%s' instance type isn't allowed within the account!", instanceTypeName));
                }
            }
        }
    }

//    private void validateNumberOfClustersPerUser(String owner, AccountPreferences preferences) throws AccountPreferencesValidationException {
//        Long maxClustersPerUser = preferences.getMaxNumberOfClustersPerUser();
//        if (needToValidateField(maxClustersPerUser)) {
//            Set<Stack> stacks = stackService.retrieveOwnerStacks(owner);
//            if (stacks.size() >= maxClustersPerUser) {
//                throw new AccountPreferencesValidationException(
//                        String.format("No more cluster could be created! The number of clusters exceeded the user's limit(%s)!", maxClustersPerUser));
//            }
//        }
//    }

    public void validateUserTimeToLive(String owner, AccountPreferences preferences) throws AccountPreferencesValidationException {
        Long userTimeToLive = preferences.getUserTimeToLive();
        if (needToValidateField(userTimeToLive)) {
            IdentityUser identityUser = cachedUserDetailsService.getDetails(owner, UserFilterField.USERID);
            long now = getTimeInMillis();
            long userActiveTime = now - identityUser.getCreated().getTime();
            if (userActiveTime > userTimeToLive) {
                throw new AccountPreferencesValidationException("The user demo time is expired!");
            }
        }
    }

    public void validateClusterTimeToLive(Long created, Long clusterTimeToLive) throws AccountPreferencesValidationException {
        if (needToValidateField(clusterTimeToLive)) {
            long now = getTimeInMillis();
            long clusterRunningTime = now - created;
            if (clusterRunningTime > clusterTimeToLive) {
                throw new AccountPreferencesValidationException("The maximum running time that is configured for the account is exceeded by the cluster!");
            }
        }
    }

    long getTimeInMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }

    private boolean needToValidateField(Long field) {
        return field != null && !EXTREMAL_VALUE.equals(field);
    }

    private boolean needToValidateField(Collection<String> field) {
        return field != null && !field.isEmpty();
    }
}