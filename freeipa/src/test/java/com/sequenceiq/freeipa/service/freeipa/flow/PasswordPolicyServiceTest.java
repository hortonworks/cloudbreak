package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    private static final String MIN_PASSWORD_LENGTH = "krbpwdminlength";

    private static final Integer MIN_PASSWORD_LENGTH_VALUE = 8;

    private static final String MIN_CHARACTER_CLASSES = "krbpwdmindiffchars";

    private static final Integer MIN_CHARACTER_CLASSES_VALUE = 3;

    private static final String MAX_PASSWORD_LIFETIME_DAYS = "krbmaxpwdlife";

    private static final Integer MAX_PASSWORD_LIFETIME_DAYS_VALUE = 90;

    private static final Integer MIN_PASSWORD_LIFETIME_HOURS_VALUE = 0;

    private static final String MAX_FAILURES_BEFORE_LOCK = "krbpwdmaxfailure";

    private static final Integer MAX_FAILURES_BEFORE_LOCK_VALUE = 10;

    private static final String FAILURE_RESET_INTERVALL_SECONDS = "krbpwdfailurecountinterval";

    private static final Integer FAILURE_RESET_INTERVALL_SECONDS_VALUE = 3;

    private static final String LOCKOUT_DURATION_SECONDS = "krbpwdlockoutduration";

    private static final Integer LOCKOUT_DURATION_SECONDS_VALUE = 10;

    @InjectMocks
    private PasswordPolicyService underTest;

    @Mock
    private FreeIpaClient freeIpaClient;

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(underTest, "minPasswordLength", MIN_PASSWORD_LENGTH_VALUE);
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", MIN_CHARACTER_CLASSES_VALUE);
        ReflectionTestUtils.setField(underTest, "minPasswordLife", MIN_PASSWORD_LIFETIME_HOURS_VALUE);
        ReflectionTestUtils.setField(underTest, "maxPasswordLife", MAX_PASSWORD_LIFETIME_DAYS_VALUE);
        ReflectionTestUtils.setField(underTest, "maxFailuresBeforeLock", MAX_FAILURES_BEFORE_LOCK_VALUE);
        ReflectionTestUtils.setField(underTest, "failureResetInterval", FAILURE_RESET_INTERVALL_SECONDS_VALUE);
        ReflectionTestUtils.setField(underTest, "lockoutDuration", LOCKOUT_DURATION_SECONDS_VALUE);
    }

    @Test
    void testUpdatePasswordPolicyShouldThrowExceptionWhenThePasswordPolicyRetrieveThrowsAnException() throws FreeIpaClientException {
        when(freeIpaClient.getPasswordPolicy()).thenThrow(new FreeIpaClientException("Error"));

        assertThrows(FreeIpaClientException.class, () -> {
            underTest.updatePasswordPolicy(freeIpaClient);
        });
    }

    @Test
    void testUpdatePasswordPolicyShouldThrowExceptionWhenThePasswordPolicyUpdateThrowsAnException() throws FreeIpaClientException {
        when(freeIpaClient.getPasswordPolicy()).thenReturn(new PasswordPolicy());
        doThrow(new FreeIpaClientException("Error")).when(freeIpaClient).updatePasswordPolicy(any());

        assertThrows(FreeIpaClientException.class, () -> {
            underTest.updatePasswordPolicy(freeIpaClient);
        });
    }

    @Test
    void testUpdatePasswordPolicyShouldUpdateAllPropertyWhenAllExistingPropertyIsNotCorrect() throws FreeIpaClientException {
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbmaxpwdlife(1);
        passwordPolicy.setKrbpwdfailurecountinterval(1);
        passwordPolicy.setKrbpwdlockoutduration(1);
        passwordPolicy.setKrbpwdmaxfailure(1);
        passwordPolicy.setKrbpwdminlength(1);
        passwordPolicy.setKrbpwdmindiffchars(1);

        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);

        underTest.updatePasswordPolicy(freeIpaClient);

        verify(freeIpaClient).getPasswordPolicy();
        ArgumentCaptor<Map<String, Object>> paramCaptor = ArgumentCaptor.forClass(Map.class);
        verify(freeIpaClient).updatePasswordPolicy(paramCaptor.capture());
        Map<String, Object> captorValue = paramCaptor.getValue();
        assertEquals(MIN_PASSWORD_LENGTH_VALUE, captorValue.get(MIN_PASSWORD_LENGTH));
        assertEquals(MIN_CHARACTER_CLASSES_VALUE, captorValue.get(MIN_CHARACTER_CLASSES));
        assertEquals(MAX_PASSWORD_LIFETIME_DAYS_VALUE, captorValue.get(MAX_PASSWORD_LIFETIME_DAYS));
        assertEquals(MAX_FAILURES_BEFORE_LOCK_VALUE, captorValue.get(MAX_FAILURES_BEFORE_LOCK));
        assertEquals(FAILURE_RESET_INTERVALL_SECONDS_VALUE, captorValue.get(FAILURE_RESET_INTERVALL_SECONDS));
        assertEquals(LOCKOUT_DURATION_SECONDS_VALUE, captorValue.get(LOCKOUT_DURATION_SECONDS));
    }

    @Test
    void testUpdatePasswordPolicyShouldUpdateAllPropertyWhenAllExistingPropertyIsNull() throws FreeIpaClientException {
        PasswordPolicy passwordPolicy = new PasswordPolicy();

        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);

        underTest.updatePasswordPolicy(freeIpaClient);

        verify(freeIpaClient).getPasswordPolicy();
        ArgumentCaptor<Map<String, Object>> paramCaptor = ArgumentCaptor.forClass(Map.class);
        verify(freeIpaClient).updatePasswordPolicy(paramCaptor.capture());
        Map<String, Object> captorValue = paramCaptor.getValue();
        assertEquals(MIN_PASSWORD_LENGTH_VALUE, captorValue.get(MIN_PASSWORD_LENGTH));
        assertEquals(MIN_CHARACTER_CLASSES_VALUE, captorValue.get(MIN_CHARACTER_CLASSES));
        assertEquals(MAX_PASSWORD_LIFETIME_DAYS_VALUE, captorValue.get(MAX_PASSWORD_LIFETIME_DAYS));
        assertEquals(MAX_FAILURES_BEFORE_LOCK_VALUE, captorValue.get(MAX_FAILURES_BEFORE_LOCK));
        assertEquals(FAILURE_RESET_INTERVALL_SECONDS_VALUE, captorValue.get(FAILURE_RESET_INTERVALL_SECONDS));
        assertEquals(LOCKOUT_DURATION_SECONDS_VALUE, captorValue.get(LOCKOUT_DURATION_SECONDS));
    }

    @Test
    void testUpdatePasswordPolicyShouldNotUpdateAllPropertyWhenAllExistingPropertyCorrect() throws FreeIpaClientException {
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbmaxpwdlife(MAX_PASSWORD_LIFETIME_DAYS_VALUE);
        passwordPolicy.setKrbminpwdlife(MIN_PASSWORD_LIFETIME_HOURS_VALUE);
        passwordPolicy.setKrbpwdfailurecountinterval(FAILURE_RESET_INTERVALL_SECONDS_VALUE);
        passwordPolicy.setKrbpwdlockoutduration(LOCKOUT_DURATION_SECONDS_VALUE);
        passwordPolicy.setKrbpwdmaxfailure(MAX_FAILURES_BEFORE_LOCK_VALUE);
        passwordPolicy.setKrbpwdminlength(MIN_PASSWORD_LENGTH_VALUE);
        passwordPolicy.setKrbpwdmindiffchars(MIN_CHARACTER_CLASSES_VALUE);

        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);

        underTest.updatePasswordPolicy(freeIpaClient);

        verify(freeIpaClient).getPasswordPolicy();
        verifyNoMoreInteractions(freeIpaClient);
    }

}