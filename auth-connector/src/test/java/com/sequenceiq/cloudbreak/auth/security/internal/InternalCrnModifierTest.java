package com.sequenceiq.cloudbreak.auth.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUser;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void before() {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        ThreadBasedUserCrnProvider.removeUserCrn();
    }

    @Test
    public void testModificationIfUserCrnIsRealUser() {
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);

        underTest.changeInternalCrn(proceedingJoinPoint);

        verify(reflectionUtil, times(0)).getParameter(any(), any(), any());
        assertEquals(USER_CRN, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    public void testModificationIfUserCrnIsNull() {
        ThreadBasedUserCrnProvider.setUserCrn(null);

        underTest.changeInternalCrn(proceedingJoinPoint);

        verify(reflectionUtil, times(0)).getParameter(any(), any(), any());
        assertNull(ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    public void testModificationIfUserCrnIsInternalButThereIsNoResourceCrnParameter() {
        ThreadBasedUserCrnProvider.setUserCrn(INTERNAL_CRN);
        when(reflectionUtil.getParameter(any(), any(), eq(ResourceCrn.class))).thenReturn(Optional.empty());

        underTest.changeInternalCrn(proceedingJoinPoint);

        assertEquals(INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    public void testModificationIfUserCrnIsInternalButResourceCrnParameterIsNotString() {
        ThreadBasedUserCrnProvider.setUserCrn(INTERNAL_CRN);
        when(reflectionUtil.getParameter(any(), any(), eq(ResourceCrn.class))).thenReturn(Optional.of(2));

        underTest.changeInternalCrn(proceedingJoinPoint);

        assertEquals(INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    public void testModificationIfUserCrnIsInternalButResourceCrnParameterIsNotCrn() {
        ThreadBasedUserCrnProvider.setUserCrn(INTERNAL_CRN);
        when(reflectionUtil.getParameter(any(), any(), eq(ResourceCrn.class))).thenReturn(Optional.of("not_crn"));

        underTest.changeInternalCrn(proceedingJoinPoint);

        assertEquals(INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Test
    public void testModificationIfUserCrnIsInternal() {
        ThreadBasedUserCrnProvider.setUserCrn(INTERNAL_CRN);
        when(reflectionUtil.getParameter(any(), any(), eq(ResourceCrn.class))).thenReturn(Optional.of(STACK_CRN));
        doNothing().when(internalUserModifier).persistModifiedInternalUser(any());

        underTest.changeInternalCrn(proceedingJoinPoint);

        assertEquals(EXPECTED_INTERNAL_CRN, ThreadBasedUserCrnProvider.getUserCrn());
        ArgumentCaptor<CrnUser> newUserCaptor = ArgumentCaptor.forClass(CrnUser.class);
        verify(internalUserModifier, times(1)).persistModifiedInternalUser(newUserCaptor.capture());
        assertEquals("1234", newUserCaptor.getValue().getTenant());
    }
}
