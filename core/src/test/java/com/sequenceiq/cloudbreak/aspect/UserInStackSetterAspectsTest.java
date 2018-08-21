package com.sequenceiq.cloudbreak.aspect;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public class UserInStackSetterAspectsTest {

    @InjectMocks
    private UserInStackSetterAspects underTest;

    @Mock
    private CachedUserDetailsService cachedUserDetailsService;

    @Mock
    private UserService userService;

    private Stack testStack;

    @Before
    public void setup() {
        initMocks(this);
        testStack = new Stack();
        when(cachedUserDetailsService.getDetails(eq(testStack.getOwner()), eq(UserFilterField.USERID))).thenReturn(null);
        when(userService.getOrCreate(any())).thenReturn(new User());
    }

    @Test
    public void testSettingUserInOptionalStack() {
        underTest.setUserInStack(Optional.of(testStack));

        verify(cachedUserDetailsService, times(1)).getDetails(eq(testStack.getOwner()), eq(UserFilterField.USERID));
        verify(userService, times(1)).getOrCreate(any());
        assertNotNull(testStack.getCreator());
    }

    @Test
    public void testSettingUserInStackList() {
        underTest.setUserInStack(Collections.singleton(testStack));

        verify(cachedUserDetailsService, times(1)).getDetails(eq(testStack.getOwner()), eq(UserFilterField.USERID));
        verify(userService, times(1)).getOrCreate(any());
        assertNotNull(testStack.getCreator());
    }

    @Test
    public void testSettingUserInStack() {
        underTest.setUserInStack(testStack);

        verify(cachedUserDetailsService, times(1)).getDetails(eq(testStack.getOwner()), eq(UserFilterField.USERID));
        verify(userService, times(1)).getOrCreate(any());
        assertNotNull(testStack.getCreator());
    }

    @Test
    public void testSettingUserInNonStackObject() {
        underTest.setUserInStack(new RDSConfig());

        verifyZeroInteractions(cachedUserDetailsService);
        verifyZeroInteractions(userService);
    }

}
