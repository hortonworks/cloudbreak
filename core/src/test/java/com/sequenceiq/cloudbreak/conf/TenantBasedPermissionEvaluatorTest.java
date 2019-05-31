package com.sequenceiq.cloudbreak.conf;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import com.sequenceiq.cloudbreak.service.security.TenantBasedPermissionEvaluator;

public class TenantBasedPermissionEvaluatorTest {

    @InjectMocks
    private TenantBasedPermissionEvaluator underTest;

    @Mock
    private Authentication oauth;

    @Before
    public void setup() {
        underTest = new TenantBasedPermissionEvaluator();
        MockitoAnnotations.initMocks(this);
        when(oauth.getPrincipal()).thenReturn("principal");
    }

    @Test
    public void testTargetNotFound() {
        boolean result  = underTest.hasPermission(null, null, "read");
        Assert.assertFalse(result);
    }
}
