package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AzureStackUtilTest {

    @InjectMocks
    private AzureStackUtil util;

    @Test
    public void testGetNextIPAddress() {
        String startIp = "172.16.0.253";

        String next1 = util.getNextIPAddress(startIp);
        String next2 = util.getNextIPAddress(next1);
        String next3 = util.getNextIPAddress(next2);

        assertEquals("172.16.0.254", next1);
        assertEquals("172.16.0.255", next2);
        assertEquals("172.16.1.0", next3);
    }

}
