package com.sequenceiq.cloudbreak.service.ha;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.domain.Node;
import com.sequenceiq.cloudbreak.ha.service.NodeService;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;

@ExtendWith(MockitoExtension.class)
public class NodeValidatorTest {

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private NodeService nodeService;

    @Mock
    private Clock clock;

    @InjectMocks
    private NodeValidator underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeDeclaredField(underTest, "nodeHeartbeatValidationEnabled", true, true);
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(TimeUnit.SECONDS.toMillis(100));
    }

    @Test
    void testCheckForRecentHeartbeatIfNodeIdNotPresent() {
        when(nodeConfig.getId()).thenReturn("");

        underTest.checkForRecentHeartbeat();
    }

    @Test
    void testCheckForRecentHeartbeatIfNodeNotPresent() {
        when(nodeConfig.getId()).thenReturn("id");
        when(nodeService.findById(any())).thenReturn(Optional.empty());

        assertThrows(CloudbreakServiceException.class, () -> underTest.checkForRecentHeartbeat());
    }

    @Test
    void testCheckForRecentHeartbeatIfNodeHeartbeatOld() {
        when(nodeConfig.getId()).thenReturn("id");
        when(nodeService.findById(any())).thenReturn(Optional.of(new Node()));

        assertThrows(CloudbreakServiceException.class, () -> underTest.checkForRecentHeartbeat());
    }

    @Test
    void testCheckForRecentHeartbeatIfNodeHeartbeatNew() {
        when(nodeConfig.getId()).thenReturn("id");
        Node node = new Node();
        node.setLastUpdated(TimeUnit.SECONDS.toMillis(90));
        when(nodeService.findById(any())).thenReturn(Optional.of(node));

        underTest.checkForRecentHeartbeat();
    }
}
