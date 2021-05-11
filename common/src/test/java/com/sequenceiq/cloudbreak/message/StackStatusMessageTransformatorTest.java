package com.sequenceiq.cloudbreak.message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

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

        String expected = "The Control Plane was not able to establish the connection with the gateway instance. This means that the reverse SSH tunnel "
                + "(autossh process) running on this instance could not connect to the Cloudera server. Please check your connection and proxy settings and "
                + "make sure the instance can reach *.ccm.cdp.cloudera.com. Refer to Cloudera documentation at "
                + DocumentationLinkProvider.ccmSetupLink();
        Assertions.assertEquals(expected, result);
    }

    @Test
    void transformMessageWhenNoPatternFound() {
        String result = underTest.transformMessage("com.sequenceiq.environment.exception.FreeIpaOperationFailedException: Random error happened");

        String expected = "com.sequenceiq.environment.exception.FreeIpaOperationFailedException: Random error happened";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void transformMessageWithEmptyMessage() {
        String result = underTest.transformMessage("");

        String expected = "";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void transformMessageWithSpaceMessage() {
        String result = underTest.transformMessage(" ");

        String expected = " ";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void transformMessageWithNullMessage() {
        String result = underTest.transformMessage(null);

        String expected = null;
        Assertions.assertEquals(expected, result);
    }
}