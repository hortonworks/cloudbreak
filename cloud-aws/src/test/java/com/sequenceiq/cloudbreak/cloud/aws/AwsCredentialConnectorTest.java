package com.sequenceiq.cloudbreak.cloud.aws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AwsCredentialConnectorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private AwsCredentialConnector underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInteractiveLoginIsProhibitedOnAws() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null, null);
    }
}
