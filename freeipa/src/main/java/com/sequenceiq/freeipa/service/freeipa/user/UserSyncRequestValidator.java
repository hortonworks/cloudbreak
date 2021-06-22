package com.sequenceiq.freeipa.service.freeipa.user;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class UserSyncRequestValidator {

    public void validateParameters(String accountId, String actorCrn, Set<String> environmentCrnFilter, UserSyncRequestFilter userSyncRequestFilter) {
        requireNonNull(accountId, "accountId must not be null");
        requireNonNull(actorCrn, "actorCrn must not be null");
        requireNonNull(environmentCrnFilter, "environmentCrnFilter must not be null");
        requireNonNull(userSyncRequestFilter);
        ImmutableSet<String> userCrnFilter = userSyncRequestFilter.getUserCrnFilter();
        ImmutableSet<String> machineUserCrnFilter = userSyncRequestFilter.getMachineUserCrnFilter();
        Optional<String> deletedWorkoadUser = userSyncRequestFilter.getDeletedWorkloadUser();
        requireNonNull(userCrnFilter, "userCrnFilter must not be null");
        requireNonNull(machineUserCrnFilter, "machineUserCrnFilter must not be null");
        requireNonNull(deletedWorkoadUser, "deletedWorkoadUser must not be null");
        validateCrnFilter(environmentCrnFilter, Crn.ResourceType.ENVIRONMENT);
        validateCrnFilter(userCrnFilter, Crn.ResourceType.USER);
        validateCrnFilter(machineUserCrnFilter, Crn.ResourceType.MACHINE_USER);
        validateSameAccount(accountId, Iterables.concat(environmentCrnFilter, userCrnFilter, machineUserCrnFilter));
        validateDeletedWorkloadUser(deletedWorkoadUser, userCrnFilter, machineUserCrnFilter);
    }

    private void validateDeletedWorkloadUser(Optional<String> deletedWorkloadUser, Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        if (deletedWorkloadUser.isPresent() && userCrnFilter.size() + machineUserCrnFilter.size() != 1) {
            throw new BadRequestException("Exactly 1 user or machine user needs to be specified when deletedWorkloadUser is present");
        }
    }

    private void validateSameAccount(String accountId, Iterable<String> crns) {
        crns.forEach(crnString -> {
            Crn crn = Crn.safeFromString(crnString);
            if (!accountId.equals(crn.getAccountId())) {
                throw new BadRequestException(String.format("Crn %s is not in the expected account %s", crnString, accountId));
            }
        });
    }

    @VisibleForTesting
    void validateCrnFilter(Set<String> crnFilter, Crn.ResourceType resourceType) {
        crnFilter.forEach(crnString -> {
            try {
                Crn crn = Crn.safeFromString(crnString);
                if (crn.getResourceType() != resourceType) {
                    throw new BadRequestException(String.format("Crn %s is not of expected type %s", crnString, resourceType));
                }
            } catch (CrnParseException e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        });
    }

}
