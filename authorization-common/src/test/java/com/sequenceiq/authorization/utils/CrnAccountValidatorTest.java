package com.sequenceiq.authorization.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;

public class CrnAccountValidatorTest {

    private static final String ACCOUNT_1 = "account1";

    private static final String ACCOUNT_2 = "account2";

    private static final String INVALID_CRN = "INVALID_CRN";

    private final CrnAccountValidator underTest = new CrnAccountValidator();

    @Test
    public void validIfUserIsInternal() {
        assertDoesNotThrow(() ->
                underTest.validateSameAccount("crn:cdp:iam:us-west-1:altus:user:__internal__actor__", environmentCrn(ACCOUNT_1)));
    }

    @Test
    public void throwsAccessDeniedIfAccountsNotMatch() {
        assertThrowsException(userCrn(ACCOUNT_1), List.of(environmentCrn(ACCOUNT_1), environmentCrn(ACCOUNT_2)), AccessDeniedException.class,
                "Can't access resource from different account.");
    }

    @Test
    public void throwsIllegalArgumentIfUserCrnIsInvalid() {
        assertThrowsException(INVALID_CRN, environmentCrn(ACCOUNT_1), IllegalArgumentException.class,
                "INVALID_CRN is not a valid user crn");
    }

    @Test
    public void throwsIllegalArgumentIfResourceCrnIsInvalid() {
        assertThrowsException(userCrn(ACCOUNT_1), INVALID_CRN, CrnParseException.class,
                "INVALID_CRN does not match the CRN pattern");
    }

    @Test
    public void throwsIllegalArgumentIfFirstParamIsNotUserCrn() {
        assertThrowsException(environmentCrn(ACCOUNT_1), environmentCrn(ACCOUNT_1), IllegalArgumentException.class,
                "crn:cdp:environments:us-west-1:account1:environment:1 is not a valid user crn");
    }

    private String userCrn(String accountId) {
        return String.format("crn:cdp:iam:us-west-1:%s:user:1", accountId);
    }

    private String environmentCrn(String accountId) {
        return String.format("crn:cdp:environments:us-west-1:%s:environment:1", accountId);
    }

    private <T extends RuntimeException> void assertThrowsException(String userCrn, String resourceCrn, Class<T> exceptionClass, String message) {
        T exception = assertThrows(exceptionClass, () -> underTest.validateSameAccount(userCrn, List.of(resourceCrn)));
        assertEquals(message, exception.getMessage());
    }

    private <T extends RuntimeException> void assertThrowsException(String userCrn, Collection<String> resourceCrns, Class<T> exceptionClass, String message) {
        T exception = assertThrows(exceptionClass, () -> underTest.validateSameAccount(userCrn, resourceCrns));
        assertEquals(message, exception.getMessage());
    }
}