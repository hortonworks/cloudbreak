package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;

import org.junit.Before;
import org.junit.Test;

public class NetworkV4BaseTest {

    private NetworkV4Base underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new NetworkV4Base();
    }

    @Test
    public void testAwsParameters() {
        assertNull(underTest.getAws());

        AwsNetworkV4Parameters parameters = underTest.createAws();
        assertNotNull(parameters);

        parameters = new AwsNetworkV4Parameters();
        underTest.setAws(parameters);
        assertEquals(parameters, underTest.createAws());
        assertEquals(parameters, underTest.getAws());
    }

}
