package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class OpenStackCredentialConnectorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private OpenStackCredentialConnector underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInteractiveLoginIsProhibitedOnOpenStack() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null);
    }
}
