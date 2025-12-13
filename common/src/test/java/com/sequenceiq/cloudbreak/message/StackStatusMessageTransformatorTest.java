package com.sequenceiq.cloudbreak.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackStatusMessageTransformatorTest {

    private StackStatusMessageTransformator underTest;

    @BeforeEach
    public void init() {
        underTest = new StackStatusMessageTransformator();
        underTest.init();
    }

    @Test
    void transformMessageWithCCMIssue() {
        String result = underTest.transformMessage("com.sequenceiq.environment.exception.FreeIpaOperationFailedException: "
                + "FreeIpa creation operation failed. FreeIpa creation failed. Status: 'CREATE_FAILED' statusReason: 'Status: 502 Bad Gateway Response: "
                + "{\"status\":502,\"code\":\"cluster-proxy.ccm.endpoint-unavailable\",\"message\":\"Unable to get endpoint from CCM for key "
                + "(i-0ff60b23ff3845f39, GATEWAY)\"}'");

        String expected = "The Control Plane was not able to establish the connection with the gateway instance. " +
                "This means that the reverse SSH tunnel (autossh process) running on this instance could not connect " +
                "to the Cloudera server. Please check your connection and proxy settings and make sure the instance " +
                "can reach *.ccm.cdp.cloudera.com. Refer to Cloudera documentation " +
                "at https://docs.cloudera.com/management-console/cloud/connection-to-private-subnets/topics/mc-ccm-troubleshooting.html";
        assertEquals(expected, result);
    }

    @Test
    void transformMessageWithCCMV2Issue() {
        String result = underTest.transformMessage("com.sequenceiq.environment.exception.FreeIpaOperationFailedException: "
                + "FreeIpa creation operation failed. FreeIpa creation failed. Status: 'CREATE_FAILED' statusReason: 'Status: 502 Bad Gateway Response: "
                + "{\"status\":502,\"code\":\"cluster-proxy.ccmv2.endpoint-unavailable\",\"message\":\"Unable to get endpoint from CCM for key "
                + "(i-0ff60b23ff3845f39, GATEWAY)\"}'");

        String expected = "The Control Plane was not able to establish the connection with the gateway instance. " +
                "This could be caused by the Jumpgate agent running on this instance not being able to connect to " +
                "the Cloudera server. Please check your connection and proxy settings and make sure the instance can " +
                "reach *.v2.ccm.cdp.cloudera.com Please check your instance on the cloud provider side if it is up and " +
                "running. Restart it if it could not start up properly.";
        assertEquals(expected, result);
    }

    @Test
    void transformMessageWhenNoPatternFound() {
        String result = underTest.transformMessage("com.sequenceiq.environment.exception.FreeIpaOperationFailedException: Random error happened");

        String expected = "com.sequenceiq.environment.exception.FreeIpaOperationFailedException: Random error happened";
        assertEquals(expected, result);
    }

    @Test
    void transformMessageWithEmptyMessage() {
        String result = underTest.transformMessage("");

        String expected = "";
        assertEquals(expected, result);
    }

    @Test
    void transformMessageWithSpaceMessage() {
        String result = underTest.transformMessage(" ");

        String expected = " ";
        assertEquals(expected, result);
    }

    @Test
    void transformMessageWithNullMessage() {
        String result = underTest.transformMessage(null);

        String expected = null;
        assertEquals(expected, result);
    }
}