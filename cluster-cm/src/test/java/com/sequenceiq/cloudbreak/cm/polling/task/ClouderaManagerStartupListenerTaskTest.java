package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerStartupListenerTaskTest {

    @InjectMocks
    private ClouderaManagerStartupListenerTask underTest;

    @Test
    public void testExtendedTimeoutMessage() {
        assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.handleTimeout(new ClouderaManagerPollerObject(new Stack(), new ApiClient())),
                "Polling of [API Echo] timed out. Please check Cloudera Manager logs and service status, " +
                        "possibly Cloudera Manager hasn't been started properly.");
    }

}
