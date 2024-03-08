package com.sequenceiq.cloudbreak.auth.crn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class AccountIdServiceTest {

    private final AccountIdService underTest = new AccountIdService();

    @Test
    void testGetAccountIdFromUserCrnShouldReturnTheAccountId() {
        assertEquals("1234", underTest.getAccountIdFromUserCrn("crn:cdp:iam:us-west-1:1234:user:1"));
    }

    @Test
    void testGetAccountIdFromUserCrnShouldThrowBadRequestWhenTheUserCrnIsNull() {
        assertThrows(BadRequestException.class, () -> underTest.getAccountIdFromUserCrn(null));
    }

    @Test
    void testGetAccountIdFromUserCrnShouldThrowBadRequestWhenTheUserCrnIsInvalid() {
        assertThrows(BadRequestException.class, () -> underTest.getAccountIdFromUserCrn("crn"));
    }

}