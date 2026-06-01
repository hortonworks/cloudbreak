package com.sequenceiq.cloudbreak.auth.crn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class AccountIdServiceTest {

    private final AccountIdService underTest = new AccountIdService();

    @Test
    void testGetAccountIdFromUserCrnShouldReturnTheAccountId() {
        assertEquals("1234", underTest.getAccountIdFromResourceCrn("crn:cdp:iam:us-west-1:1234:user:1"));
    }

    @Test
    void testGetAccountIdFromUserCrnShouldThrowBadRequestWhenTheResourceCrnIsNull() {
        assertThrows(BadRequestException.class, () -> underTest.getAccountIdFromResourceCrn(null));
    }

    @Test
    void testGetAccountIdFromUserCrnShouldThrowBadRequestWhenTheResourceCrnIsInvalid() {
        assertThrows(BadRequestException.class, () -> underTest.getAccountIdFromResourceCrn("crn"));
    }

}