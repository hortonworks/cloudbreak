package com.sequenceiq.cloudbreak.conf;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

public class OwnerBasedPermissionEvaluatorTest {

    @InjectMocks
    private OwnerBasedPermissionEvaluator underTest;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private OAuth2Authentication oauth;

    private Stack stack;

    @Before
    public void setup() {
        underTest = new OwnerBasedPermissionEvaluator();
        MockitoAnnotations.initMocks(this);
        when(oauth.getPrincipal()).thenReturn("principal");
        stack = TestUtil.stack();
    }

    @Test(expected = NotFoundException.class)
    public void testTargetNotFound() {
        underTest.hasPermission(null, null, "read");
    }

    @Test
    public void testWriteOwner() {
        when(oauth.getUserAuthentication()).thenReturn(new TestingAuthenticationToken("principal", "credential"));
        CbUser user = new CbUser("userid", "", "", Collections.emptyList(), "", "", null);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(user);

        boolean result = underTest.hasPermission(oauth, stack, "write");

        Assert.assertTrue(result);
    }

    @Test
    public void testWriteNotOwnerButAdmin() {
        when(oauth.getUserAuthentication()).thenReturn(new TestingAuthenticationToken("principal", "credential"));
        CbUser user = new CbUser("admin", "", "account", Collections.singletonList(CbUserRole.ADMIN), "", "", null);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(user);

        boolean result = underTest.hasPermission(oauth, stack, "write");

        Assert.assertTrue(result);
    }

    @Test
    public void testReadNotOwnerNotAdminButPublicInAccount() {
        when(oauth.getUserAuthentication()).thenReturn(new TestingAuthenticationToken("principal", "credential"));
        CbUser user = new CbUser("admin", "", "account", Collections.emptyList(), "", "", null);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(user);
        stack.setPublicInAccount(true);
        boolean result = underTest.hasPermission(oauth, stack, "read");

        Assert.assertTrue(result);
    }

    @Test
    public void testReadNotOwnerNotAdminNotAccountButPublicInAccount() {
        when(oauth.getUserAuthentication()).thenReturn(new TestingAuthenticationToken("principal", "credential"));
        CbUser user = new CbUser("admin", "", "test-account", Collections.emptyList(), "", "", null);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(user);
        stack.setPublicInAccount(true);
        boolean result = underTest.hasPermission(oauth, stack, "read");

        Assert.assertFalse(result);
    }

    @Test
    public void testWriteNotOwnerNotAdminButPublicInAccount() {
        when(oauth.getUserAuthentication()).thenReturn(new TestingAuthenticationToken("principal", "credential"));
        CbUser user = new CbUser("admin", "", "account", Collections.emptyList(), "", "", null);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(user);
        stack.setPublicInAccount(true);
        boolean result = underTest.hasPermission(oauth, stack, "write");

        Assert.assertFalse(result);
    }

    @Test
    public void testReadNotOwnerNotAdminNotPublicInAccount() {
        when(oauth.getUserAuthentication()).thenReturn(new TestingAuthenticationToken("principal", "credential"));
        CbUser user = new CbUser("admin", "", "account", Collections.emptyList(), "", "", null);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(user);

        boolean result = underTest.hasPermission(oauth, stack, "read");

        Assert.assertFalse(result);
    }

    @Test
    public void testWriteAutoScale() {
        when(oauth.getUserAuthentication()).thenReturn(null);
        OAuth2Request oAuth2Request = new OAuth2Request(null, null, null, true, Collections.singleton("cloudbreak.autoscale"), null, null, null, null);
        when(oauth.getOAuth2Request()).thenReturn(oAuth2Request);

        boolean result = underTest.hasPermission(oauth, stack, "write");

        Assert.assertTrue(result);
    }

    @Test
    public void testReadNotAutoScale() {
        when(oauth.getUserAuthentication()).thenReturn(null);
        OAuth2Request oAuth2Request = new OAuth2Request(null, null, null, true, Collections.singleton("cloudbreak.not.autoscale"), null, null, null, null);
        when(oauth.getOAuth2Request()).thenReturn(oAuth2Request);

        boolean result = underTest.hasPermission(oauth, stack, "read");

        Assert.assertFalse(result);
    }
}
