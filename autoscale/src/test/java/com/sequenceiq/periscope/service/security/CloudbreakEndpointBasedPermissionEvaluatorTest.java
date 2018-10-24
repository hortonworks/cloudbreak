package com.sequenceiq.periscope.service.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@RunWith(MockitoJUnitRunner.class)
public class CloudbreakEndpointBasedPermissionEvaluatorTest {

    private static final String READ = "READ";

    @InjectMocks
    private CloudbreakEndpointBasedPermissionEvaluator underTest;

    @Mock
    private CachedUserDetailsService cachedUserDetailsService;

    @Mock
    private StackSecurityService stackSecurityService;

    @Mock
    private OAuth2Authentication authentication;

    private final PeriscopeUser periscopeUserOk = new PeriscopeUser("owner", "email", "tenant");

    private final PeriscopeUser periscopeUserBad = new PeriscopeUser("owner-bad", "email", "tenant");

    @Before
    public void setup() {
        when(authentication.getPrincipal()).thenReturn("owner");
        when(cachedUserDetailsService.getDetails(eq("owner"), eq("tenant"), eq(UserFilterField.USERNAME))).thenReturn(periscopeUserOk);
    }

    @Test
    public void testHasAccessClusterAllowed() {
        when(stackSecurityService.hasAccess(eq(1L), eq(periscopeUserOk.getId()), eq(READ))).thenReturn(true);

        Cluster target = new Cluster();
        target.setStackId(1L);
        target.setUser(periscopeUserOk);

        Assert.assertTrue(underTest.hasPermission(authentication, target, READ));

        verify(cachedUserDetailsService, times(0)).getDetails(anyString(), anyString(), any(UserFilterField.class));
        verify(stackSecurityService, times(1)).hasAccess(eq(1L), eq(periscopeUserOk.getId()), eq(READ));
    }

    @Test
    public void testHasAccessClusterDeclined() {
        when(stackSecurityService.hasAccess(eq(1L), eq(periscopeUserBad.getId()), eq(READ))).thenReturn(false);

        Cluster target = new Cluster();
        target.setStackId(1L);
        target.setUser(periscopeUserBad);

        Assert.assertFalse(underTest.hasPermission(authentication, target, READ));

        verify(cachedUserDetailsService, times(0)).getDetails(anyString(), anyString(), any(UserFilterField.class));
        verify(stackSecurityService, times(1)).hasAccess(eq(1L), eq(periscopeUserBad.getId()), eq(READ));
    }

    @Test
    public void testHasAccessHistoryAllowed() {
        History target = new History();
        target.setUser(periscopeUserOk.getId());

        Assert.assertTrue(underTest.hasPermission(authentication, target, READ));

        verify(cachedUserDetailsService, times(1)).getDetails(anyString(), anyString(), any(UserFilterField.class));
        verify(stackSecurityService, times(0)).hasAccess(anyLong(), anyString(), eq(READ));
    }

    @Test
    public void testHasAccessHistoryDeclined() {
        History target = new History();
        target.setUser(periscopeUserBad.getId());

        Assert.assertFalse(underTest.hasPermission(authentication, target, READ));

        verify(cachedUserDetailsService, times(1)).getDetails(anyString(), anyString(), any(UserFilterField.class));
        verify(stackSecurityService, times(0)).hasAccess(anyLong(), anyString(), eq(READ));
    }

    @Test
    public void testHasAccessPeriscopeUserAllowed() {
        Assert.assertTrue(underTest.hasPermission(authentication, periscopeUserOk, READ));

        verify(cachedUserDetailsService, times(0)).getDetails(anyString(), anyString(), any(UserFilterField.class));
        verify(stackSecurityService, times(0)).hasAccess(anyLong(), anyString(), eq(READ));
    }
}
