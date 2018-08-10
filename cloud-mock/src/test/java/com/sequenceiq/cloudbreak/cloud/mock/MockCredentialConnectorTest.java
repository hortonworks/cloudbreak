package com.sequenceiq.cloudbreak.cloud.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class MockCredentialConnectorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private MockCredentialConnector underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInteractiveLoginIsProhibitedOnMock() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null);
    }
}
