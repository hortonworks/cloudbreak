package com.sequenceiq.cloudbreak.cloud.yarn.auth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class YarnCredentialConnectorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private YarnCredentialConnector underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInteractiveLoginIsProhibitedOnYarn() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null);
    }
}
