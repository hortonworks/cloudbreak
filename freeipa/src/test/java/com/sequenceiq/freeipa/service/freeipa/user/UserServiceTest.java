package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String NOT_CRN = "not:a:crn:";

    private static final String OTHER_CRN = "crn:altus:environments:us-west-1:"
            + ACCOUNT_ID + ":database:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:altus:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String MACHINE_USER_CRN = "crn:altus:iam:us-west-1:"
            + ACCOUNT_ID + ":machineUser:" + UUID.randomUUID().toString();

    @InjectMocks
    UserService underTest;

    @Test
    void testValidateParameters() {
        underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(ENV_CRN), Set.of(USER_CRN), Set.of(MACHINE_USER_CRN));
    }

    @Test
    void testValidateParametersBadEnv() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(OTHER_CRN), Set.of(), Set.of());
        });
    }

    @Test
    void testValidateParametersBadUser() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), Set.of(OTHER_CRN), Set.of());
        });
    }

    @Test
    void testValidateParametersBadMachineUser() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), Set.of(), Set.of(OTHER_CRN));
        });
    }

    @Test
    void testValidateCrnFilter() {
        underTest.validateCrnFilter(Set.of(ENV_CRN), Crn.ResourceType.ENVIRONMENT);
    }

    @Test
    void testValidateCrnFilterNotCrn() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateCrnFilter(Set.of(NOT_CRN), Crn.ResourceType.ENVIRONMENT);
        });
    }

    @Test
    void testValidateCrnFilterWrongResourceType() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateCrnFilter(Set.of(ENV_CRN), Crn.ResourceType.USER);
        });
    }
}