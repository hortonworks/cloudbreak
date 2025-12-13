package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class UserSyncRequestValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String NOT_CRN = "not:a:crn:";

    private static final String OTHER_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":database:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String MACHINE_USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":machineUser:" + UUID.randomUUID().toString();

    private static final String WORKLOAD_USER = "workloadUser";

    @InjectMocks
    UserSyncRequestValidator underTest;

    @Test
    void testValidateParameters() {
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(USER_CRN), Set.of(MACHINE_USER_CRN), Optional.empty());
        underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(ENV_CRN), filter);
    }

    @Test
    void testValidateParametersBadEnv() {
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(OTHER_CRN), filter);
        });
    }

    @Test
    void testValidateParametersBadUser() {
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(OTHER_CRN), Set.of(), Optional.empty());
        assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), filter);
        });
    }

    @Test
    void testValidateParametersBadMachineUser() {
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(), Set.of(OTHER_CRN), Optional.empty());
        assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), filter);
        });
    }

    @Test
    void testValidateParametersWrongAccount() {
        String differentAccount = UUID.randomUUID().toString();
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(differentAccount, USER_CRN, Set.of(ENV_CRN), filter);
        });
    }

    @Test
    void testValidateDeleteUsersRequest() {
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(USER_CRN), Set.of(), Optional.of(WORKLOAD_USER));
        underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), filter);
    }

    @Test
    void testValidateParametersInvalidDeleteUsersRequest() {
        UserSyncRequestFilter filter = new UserSyncRequestFilter(Set.of(USER_CRN), Set.of(MACHINE_USER_CRN), Optional.of(WORKLOAD_USER));
        assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), filter);
        });
    }

    @Test
    void testValidateCrnFilter() {
        underTest.validateCrnFilter(Set.of(ENV_CRN), Crn.ResourceType.ENVIRONMENT);
    }

    @Test
    void testValidateCrnFilterNotCrn() {
        assertThrows(BadRequestException.class, () -> {
            underTest.validateCrnFilter(Set.of(NOT_CRN), Crn.ResourceType.ENVIRONMENT);
        });
    }

    @Test
    void testValidateCrnFilterWrongResourceType() {
        assertThrows(BadRequestException.class, () -> {
            underTest.validateCrnFilter(Set.of(ENV_CRN), Crn.ResourceType.USER);
        });
    }
}