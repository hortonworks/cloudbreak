package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.FreeIpaGroupType;
import com.sequenceiq.freeipa.client.operation.AbstractFreeipaOperation;
import com.sequenceiq.freeipa.client.operation.GroupAddMemberOperation;
import com.sequenceiq.freeipa.client.operation.GroupAddOperation;
import com.sequenceiq.freeipa.client.operation.GroupRemoveMemberOperation;
import com.sequenceiq.freeipa.client.operation.GroupRemoveOperation;
import com.sequenceiq.freeipa.client.operation.UserAddOperation;
import com.sequenceiq.freeipa.client.operation.UserDisableOperation;
import com.sequenceiq.freeipa.client.operation.UserEnableOperation;
import com.sequenceiq.freeipa.client.operation.UserRemoveOperation;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.util.ThreadInterruptChecker;

@Component
public class UserSyncOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncOperations.class);

    @Value("${freeipa.usersync.max-subjects-per-request}")
    private int maxSubjectsPerRequest;

    @Inject
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Inject
    private ThreadInterruptChecker threadInterruptChecker;

    @Inject
    private UserMetadataConverter userMetadataConverter;

    public void addGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<GroupAddOperation> posixOperations = Lists.newArrayList();
        List<GroupAddOperation> nonPosixOperations = Lists.newArrayList();
        for (FmsGroup fmsGroup : fmsGroups) {
            threadInterruptChecker.throwTimeoutExIfInterrupted();
            String groupName = fmsGroup.getName();
            if (isNonPosixGroup(groupName)) {
                nonPosixOperations.add(GroupAddOperation.create(groupName, FreeIpaGroupType.NONPOSIX, warnings));
            } else {
                posixOperations.add(GroupAddOperation.create(groupName, FreeIpaGroupType.POSIX, warnings));
            }
        }
        invokeOperation(posixOperations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), false);
        invokeOperation(nonPosixOperations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), false);
    }

    private boolean isNonPosixGroup(String groupName) {
        return UserSyncConstants.NON_POSIX_GROUPS.contains(groupName);
    }

    public void addUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsUser> fmsUsers,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<UserAddOperation> operations = Lists.newArrayList();
        for (FmsUser fmsUser : fmsUsers) {
            threadInterruptChecker.throwTimeoutExIfInterrupted();
            String userMetadataJson = userMetadataConverter.toUserMetadataJson(fmsUser.getCrn(), -1L);
            operations.add(UserAddOperation.create(fmsUser.getName(), fmsUser.getFirstName(), fmsUser.getLastName(),
                    fmsUser.getState() == FmsUser.State.DISABLED, Optional.of(userMetadataJson)));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), true);
    }

    public void disableUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<String> users,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<UserDisableOperation> operations = Lists.newArrayList();
        for (String user : users) {
            threadInterruptChecker.throwTimeoutExIfInterrupted();
            operations.add(UserDisableOperation.create(user));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(FreeIpaErrorCodes.ALREADY_INACTIVE), true);
    }

    public void enableUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<String> users,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<UserEnableOperation> operations = Lists.newArrayList();
        for (String user : users) {
            threadInterruptChecker.throwTimeoutExIfInterrupted();
            operations.add(UserEnableOperation.create(user));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(FreeIpaErrorCodes.ALREADY_ACTIVE), true);
    }

    public void removeUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<String> fmsUsers,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<UserRemoveOperation> operations = Lists.newArrayList();
        for (String user : fmsUsers) {
            threadInterruptChecker.throwTimeoutExIfInterrupted();
            operations.add(UserRemoveOperation.create(user));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(FreeIpaErrorCodes.NOT_FOUND), true);
    }

    public void removeGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<GroupRemoveOperation> operations = Lists.newArrayList();
        for (FmsGroup fmsGroup : fmsGroups) {
            threadInterruptChecker.throwTimeoutExIfInterrupted();
            operations.add(GroupRemoveOperation.create(fmsGroup.getName(), warnings));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,  Set.of(FreeIpaErrorCodes.NOT_FOUND), false);
    }

    public void addUsersToGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<GroupAddMemberOperation> operations = Lists.newArrayList();
        for (String group : groupMapping.keySet()) {
            for (List<String> users : Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest)) {
                threadInterruptChecker.throwTimeoutExIfInterrupted();
                operations.add(GroupAddMemberOperation.create(group, users, warnings));
            }
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(), false);
    }

    public void removeUsersFromGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping,
            BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        List<GroupRemoveMemberOperation> operations = Lists.newArrayList();
        for (String group : groupMapping.keySet()) {
            for (List<String> users : Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest)) {
                threadInterruptChecker.throwTimeoutExIfInterrupted();
                operations.add(GroupRemoveMemberOperation.create(group, users, warnings));
            }
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings, Set.of(), false);
    }

    private <T extends AbstractFreeipaOperation<?>> void invokeOperation(List<T> operations, boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeipaClient,
            BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes, boolean localErrorHandling)
            throws FreeIpaClientException, TimeoutException {
        if (fmsToFreeipaBatchCallEnabled) {
            List<Object> batchCallOperations = operations.stream().map(operation -> operation.getOperationParamsForBatchCall()).collect(Collectors.toList());
            String operationName = operations.stream().map(AbstractFreeipaOperation::getOperationName).findFirst().orElse("unknown");
            Integer partitionSize = batchPartitionSizeProperties.getByOperation(operationName);
            freeipaClient.callBatch(warnings, batchCallOperations, partitionSize, acceptableErrorCodes,
                    () -> threadInterruptChecker.throwTimeoutExIfInterrupted());
        } else {
            for (T operation : operations) {
                threadInterruptChecker.throwTimeoutExIfInterrupted();
                try {
                    operation.invoke(freeipaClient);
                } catch (FreeIpaClientException e) {
                    singleOperationErrorHandling(freeipaClient, warnings, acceptableErrorCodes, localErrorHandling, operation, e);
                }
            }
        }
    }

    private void singleOperationErrorHandling(FreeIpaClient freeipaClient, BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes,
            boolean localErrorHandling, AbstractFreeipaOperation<?> operation, FreeIpaClientException e) throws FreeIpaClientException {
        if (localErrorHandling) {
            if (FreeIpaClientExceptionUtil.isExceptionWithErrorCode(e, acceptableErrorCodes)) {
                LOGGER.debug(String.format("Operation %s failed with acceptable error: %s", operation.getOperationName(), e.getMessage()));
            } else {
                LOGGER.warn(e.getMessage());
                warnings.accept(String.format("operation %s failed", operation.getOperationName()), e.getMessage());
                freeipaClient.checkIfClientStillUsable(e);
            }
        } else {
            throw e;
        }
    }
}
