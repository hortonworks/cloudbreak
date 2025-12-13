package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;

class NetworkV4StackBaseTest {

    private NetworkV4StackBase underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = new NetworkV4StackBase();
    }

    @Test
    void testAwsParameters() {
        assertNull(underTest.getAws());

        AwsNetworkV4Parameters parameters = underTest.createAws();
        assertNotNull(parameters);

        parameters = new AwsNetworkV4Parameters();
        underTest.setAws(parameters);
        assertEquals(parameters, underTest.createAws());
        assertEquals(parameters, underTest.getAws());
    }

}
