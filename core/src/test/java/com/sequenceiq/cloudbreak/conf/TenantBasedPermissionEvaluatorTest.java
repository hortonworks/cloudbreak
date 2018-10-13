package com.sequenceiq.cloudbreak.conf;

import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.security.TenantBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

public class TenantBasedPermissionEvaluatorTest {

    @InjectMocks
    private TenantBasedPermissionEvaluator underTest;

    @Mock
    private CachedUserDetailsService cachedUserDetailsService;

    @Mock
    private OAuth2Authentication oauth;

    private Stack stack;

    @Before
    public void setup() {
        underTest = new TenantBasedPermissionEvaluator();
        MockitoAnnotations.initMocks(this);
        when(oauth.getPrincipal()).thenReturn("principal");
        stack = TestUtil.stack();
    }

    @Test
    public void testTargetNotFound() {
        boolean result  = underTest.hasPermission(null, null, "read");
        Assert.assertFalse(result);
    }

    @Test
    public void testTenantMatches() {

    }

    @Test
    public void testTenantNotMatches() {

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
