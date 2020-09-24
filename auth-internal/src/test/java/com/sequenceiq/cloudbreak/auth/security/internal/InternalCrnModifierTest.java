package com.sequenceiq.cloudbreak.auth.security.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@ExtendWith(MockitoExtension.class)
public class InternalCrnModifierTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String EXPECTED_INTERNAL_CRN = "crn:cdp:iam:us-west-1:1234:user:__internal__actor__";

    @Mock
    private ReflectionUtil reflectionUtil;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private InternalUserModifier internalUserModifier;

    @InjectMocks
    private InternalCrnModifier underTest;

    @BeforeEach
    public void before() {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    public void testModificationIfUserCrnIsRealUser() {
        AtomicBoolean assertationHappened = new AtomicBoolean(false);
        when(reflectionUtil.proceed(any())).thenAnswer(invocation -> {
            assertEquals(USER_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            assertationHappened.set(true);
            return null;
        });

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.changeInternalCrn(proceedingJoinPoint);
        });

        verify(reflectionUtil, times(0)).getParameter(any(), any(), any());
        assertTrue(assertationHappened.get());
    }

    @Test
    public void testModificationIfUserCrnIsNull() {
        AtomicBoolean assertationHappened = new AtomicBoolean(false);
        when(reflectionUtil.proceed(any())).thenAnswer(invocation -> {
            assertNull(ThreadBasedUserCrnProvider.getUserCrn());
            assertationHappened.set(true);
            return null;
        });

        ThreadBasedUserCrnProvider.doAs(null, () -> {
            underTest.changeInternalCrn(proceedingJoinPoint);
        });

        verify(reflectionUtil, times(0)).getParameter(any(), any(), any());
        assertTrue(assertationHappened.get());
    }

    @Test
    public void testModificationIfUserCrnIsInternalButThereIsNoResourceCrnParameter() {
        when(reflectionUtil.getParameter(any(), any(), eq(AccountId.class))).thenReturn(Optional.empty());
        when(reflectionUtil.getParameter(any(), any(), eq(TenantAwareParam.class))).thenReturn(Optional.empty());

        AtomicBoolean assertationHappened = new AtomicBoolean(false);
        when(reflectionUtil.proceed(any())).thenAnswer(invocation -> {
            assertEquals(INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            assertationHappened.set(true);
            return null;
        });

        ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN, () -> {
            underTest.changeInternalCrn(proceedingJoinPoint);
        });

        verify(reflectionUtil, times(1)).proceed(any());
        assertTrue(assertationHappened.get());
    }

    @Test
    public void testModificationIfUserCrnIsInternalButResourceCrnParameterIsNotString() {
        when(reflectionUtil.getParameter(any(), any(), eq(AccountId.class))).thenReturn(Optional.empty());
        when(reflectionUtil.getParameter(any(), any(), eq(TenantAwareParam.class))).thenReturn(Optional.of(2));

        AtomicBoolean assertationHappened = new AtomicBoolean(false);
        when(reflectionUtil.proceed(any())).thenAnswer(invocation -> {
            assertEquals(INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            assertationHappened.set(true);
            return null;
        });

        ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN, () -> {
            underTest.changeInternalCrn(proceedingJoinPoint);
        });

        verify(reflectionUtil, times(1)).proceed(any());
        assertTrue(assertationHappened.get());
    }

    @Test
    public void testModificationIfUserCrnIsInternalButResourceCrnParameterIsNotCrn() {
        when(reflectionUtil.getParameter(any(), any(), eq(AccountId.class))).thenReturn(Optional.empty());
        when(reflectionUtil.getParameter(any(), any(), eq(TenantAwareParam.class))).thenReturn(Optional.of("not_crn"));

        AtomicBoolean assertationHappened = new AtomicBoolean(false);
        when(reflectionUtil.proceed(any())).thenAnswer(invocation -> {
            assertEquals(INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            assertationHappened.set(true);
            return null;
        });

        ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN, () -> {
            underTest.changeInternalCrn(proceedingJoinPoint);
        });

        verify(reflectionUtil, times(1)).proceed(any());
        assertTrue(assertationHappened.get());
    }

    @Test
    public void testModificationIfUserCrnIsInternal() {
        when(reflectionUtil.getParameter(any(), any(), eq(AccountId.class))).thenReturn(Optional.empty());
        when(reflectionUtil.getParameter(any(), any(), eq(TenantAwareParam.class))).thenReturn(Optional.of(STACK_CRN));
        doNothing().when(internalUserModifier).persistModifiedInternalUser(any());

        AtomicBoolean assertationHappened = new AtomicBoolean(false);
        when(reflectionUtil.proceed(any())).thenAnswer(invocation -> {
            assertEquals(EXPECTED_INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            assertationHappened.set(true);
            return null;
        });

        ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN, () -> {
            underTest.changeInternalCrn(proceedingJoinPoint);
        });

        ArgumentCaptor<CrnUser> newUserCaptor = ArgumentCaptor.forClass(CrnUser.class);
        verify(internalUserModifier, times(1)).persistModifiedInternalUser(newUserCaptor.capture());
        assertEquals("1234", newUserCaptor.getValue().getTenant());
        verify(reflectionUtil, times(1)).proceed(any());
        assertTrue(assertationHappened.get());
    }
}
